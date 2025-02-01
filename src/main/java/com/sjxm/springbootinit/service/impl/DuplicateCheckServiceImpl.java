package com.sjxm.springbootinit.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.sjxm.springbootinit.common.ErrorCode;
import com.sjxm.springbootinit.exception.BusinessException;
import com.sjxm.springbootinit.mapper.DuplicateCheckMapper;
import com.sjxm.springbootinit.model.entity.DuplicateCheck;
import com.sjxm.springbootinit.model.entity.Homework;
import com.sjxm.springbootinit.model.enums.CheckStatusEnum;
import com.sjxm.springbootinit.service.DuplicateCheckService;
import com.sjxm.springbootinit.service.HomeworkService;
import com.sjxm.springbootinit.utils.AliOssUtil;
import com.sjxm.springbootinit.utils.VideoPostGenerateUtil;
import com.sjxm.springbootinit.utils.similarity.CodeSimilarityUtil;
import com.sjxm.springbootinit.utils.similarity.ConsineSimilarityUtil;
import com.sjxm.springbootinit.utils.similarity.SimHashUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StopWatch;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DuplicateCheckServiceImpl extends ServiceImpl<DuplicateCheckMapper, DuplicateCheck>
        implements DuplicateCheckService {

    private static final int TEXT_CHECK_TIMEOUT = 30;   // 文本查重超时时间30秒
    private static final int DOC_CHECK_TIMEOUT = 240;   // 文档查重超时时间4分钟
    private static final int CODE_CHECK_TIMEOUT = 240;  // 代码查重超时时间4分钟
    private static final int BATCH_SIZE = 30;          // 批处理大小
    private static final int PARTITION_SIZE = 5;       // 分区大小

    private final RateLimiter rateLimiter = RateLimiter.create(10.0);

    @Resource
    private HomeworkService homeworkService;

    @Resource
    private ThreadPoolExecutor duplicateCheckThreadPool;

    @Resource
    private AliOssUtil aliOssUtil;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void getSimilarity(Long homeworkId) {
        try {
            // 限流控制
            if (!rateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
            }

            // 获取并校验作业
            Homework source = validateAndGetHomework(homeworkId);

            // 生成视频封面
            generateVideoPost(source);

            // 执行查重
            performDuplicateCheck(source);

        } catch (Exception e) {
            log.error("作业查重失败 homeworkId=[{}]", homeworkId, e);
            throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR, "查重失败: " + e.getMessage());
        }
    }

    private Homework validateAndGetHomework(Long homeworkId) {
        Homework source = homeworkService.getById(homeworkId);
        if (source == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "作业不存在");
        }

        // 乐观锁更新
        LambdaUpdateWrapper<Homework> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Homework::getId, homeworkId)
                .eq(Homework::getVersion, source.getVersion())
                .set(Homework::getVersion, source.getVersion() + 1);

        boolean updated = homeworkService.update(updateWrapper);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "作业已被其他进程修改");
        }

        return source;
    }

    private void generateVideoPost(Homework source) {
        try {
            CompletableFuture<String> videoPostFuture = CompletableFuture.supplyAsync(() -> {
                String attachmentMp4 = source.getAttachmentMp4();
                if (!StrUtil.isBlankIfStr(attachmentMp4)) {
                    String[] videoPaths = attachmentMp4.split(",");
                    if (videoPaths.length > 0) {
                        return VideoPostGenerateUtil.extractAndUploadThumbnail(videoPaths[0], aliOssUtil);
                    }
                }
                return "";
            }, duplicateCheckThreadPool);

            String post = videoPostFuture.get(120, TimeUnit.SECONDS);
            source.setPost(post);
            homeworkService.updateById(source);
        } catch (Exception e) {
            log.error("生成视频封面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "生成视频封面失败: " + e.getMessage());
        }
    }

    private void performDuplicateCheck(Homework source) {
        // 使用局部线程安全集合存储结果
        List<DuplicateCheck> results = Collections.synchronizedList(new ArrayList<>());

        // 删除旧的查重记录
        deleteOldRecords(source.getId());

        try {
            // 分页处理所有作业
            processAllHomeworksBatch(source, results);
        } catch (Exception e) {
            log.error("查重过程发生异常", e);
            throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR, "查重失败: " + e.getMessage());
        }
    }

    private void deleteOldRecords(Long sourceId) {
        transactionTemplate.execute(status -> {
            try {
                LambdaQueryWrapper<DuplicateCheck> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(DuplicateCheck::getSourceId, sourceId);
                this.remove(queryWrapper);
                return null;
            } catch (Exception e) {
                log.error("删除旧查重记录失败", e);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除旧查重记录失败");
            }
        });
    }

    private void processAllHomeworksBatch(Homework source, List<DuplicateCheck> results) {
        int offset = 1;
        while (true) {
            Page<Homework> page = new Page<>(offset, BATCH_SIZE);
            List<Homework> batch = homeworkService.page(page).getRecords();

            if (CollUtil.isEmpty(batch)) {
                break;
            }

            processBatch(batch, source, results);
            saveResults(results);
            results.clear();

            offset++;
            if (batch.size() < BATCH_SIZE) {
                break;
            }
        }
    }

    private void processBatch(List<Homework> batch, Homework source, List<DuplicateCheck> results) {
        if (CollUtil.isEmpty(batch)) {
            return;
        }

        List<List<Homework>> partitions = Lists.partition(batch, PARTITION_SIZE);
        for (List<Homework> partition : partitions) {
            processPartition(partition, source, results);
        }
    }

    private void processPartition(List<Homework> partition, Homework source, List<DuplicateCheck> results) {
        try {
            List<CompletableFuture<DuplicateCheck>> futures = partition.stream()
                    .filter(homework -> !Objects.equals(homework.getId(), source.getId()))
                    .map(homework -> CompletableFuture.supplyAsync(
                            () -> duplicateCheck(source, homework),
                            duplicateCheckThreadPool
                    ))
                    .collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(PARTITION_SIZE * 60L, TimeUnit.SECONDS);

            for (CompletableFuture<DuplicateCheck> future : futures) {
                try {
                    DuplicateCheck result = future.get(1, TimeUnit.SECONDS);
                    if (result != null) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    log.error("获取查重结果失败", e);
                }
            }
        } catch (TimeoutException e) {
            log.error("分区处理超时", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查重处理超时");
        } catch (Exception e) {
            log.error("分区处理失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查重处理失败");
        }
    }

    private DuplicateCheck duplicateCheck(Homework source, Homework target) {
        String sourceId = source.getId().toString();
        String targetId = target.getId().toString();
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        log.info("开始查重作业 sourceId=[{}] targetId=[{}]", sourceId, targetId);

        DuplicateCheck duplicateCheck = new DuplicateCheck();
        duplicateCheck.setSourceId(source.getId());
        duplicateCheck.setTargetId(target.getId());

        try {
            // 异步计算各部分相似度
//            CompletableFuture<BigDecimal> briefFuture = calculateTextSimilarity(
//                    "简介", source.getBrief(), target.getBrief());
//            CompletableFuture<BigDecimal> backgroundFuture = calculateTextSimilarity(
//                    "背景", source.getBackground(), target.getBackground());
//            CompletableFuture<BigDecimal> systemDesignFuture = calculateTextSimilarity(
//                    "系统设计", source.getSystemDesign(), target.getSystemDesign());
//            CompletableFuture<BigDecimal> wordFuture = calculateDocumentSimilarity(
//                    "Word", source.getAttachmentWord(), target.getAttachmentWord(), "docx");
//            CompletableFuture<BigDecimal> pdfFuture = calculateDocumentSimilarity(
//                    "PDF", source.getAttachmentPdf(), target.getAttachmentPdf(), "pdf");
//            CompletableFuture<BigDecimal> sourceFuture = calculateSourceCodeSimilarity(
//                    source.getAttachmentSource(), target.getAttachmentSource());

            double brief = ConsineSimilarityUtil.calculate(source.getBrief(), target.getBrief());
            double background = ConsineSimilarityUtil.calculate(source.getBackground(), target.getBackground());
            double systemDesign = ConsineSimilarityUtil.calculate(source.getSystemDesign(), target.getSystemDesign());
            double word = SimHashUtil.calculateGroupSimilarity(source.getAttachmentWord(), target.getAttachmentWord(),"docx");
            double pdf = SimHashUtil.calculateGroupSimilarity(source.getAttachmentPdf(), target.getAttachmentPdf(),"pdf");
            double codeSource = CodeSimilarityUtil.calculateCodeSimilarity(source.getAttachmentSource(), target.getAttachmentSource());

            // 获取计算结果
            duplicateCheck.setBriefValue(BigDecimal.valueOf(brief));
            duplicateCheck.setBackgroundValue(BigDecimal.valueOf(background));
            duplicateCheck.setSystemDesignValue(BigDecimal.valueOf(systemDesign));
            duplicateCheck.setWordValue(BigDecimal.valueOf(word));
            duplicateCheck.setPdfValue(BigDecimal.valueOf(pdf));
            duplicateCheck.setSourceValue(BigDecimal.valueOf(codeSource));

            // 计算总相似度
            BigDecimal similarity = calculateTotalSimilarity(duplicateCheck);
            duplicateCheck.setSimilarity(similarity);

            stopWatch.stop();
            log.info("查重完成: sourceId=[{}], similarity=[{}], 耗时={}",
                    sourceId, similarity, stopWatch.getTotalTimeSeconds());
            return duplicateCheck;

        } catch (Exception e) {
            log.error("查重过程发生异常: sourceId=[{}]", sourceId, e);
            throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR, "查重失败: " + e.getMessage());
        }
    }

    private void saveResults(List<DuplicateCheck> results) {
        if (CollUtil.isEmpty(results)) {
            return;
        }

        Lists.partition(results, BATCH_SIZE).forEach(batch -> {
            try {
                transactionTemplate.execute(status -> {
                    this.saveBatch(batch, BATCH_SIZE);
                    return null;
                });
            } catch (Exception e) {
                log.error("保存查重结果失败", e);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存查重结果失败");
            }
        });
    }

    // 计算相似度的相关方法保持不变...
    private CompletableFuture<BigDecimal> calculateTextSimilarity(String type, String text1, String text2) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                double similarity = ConsineSimilarityUtil.calculate(text1, text2);
                return BigDecimal.valueOf(similarity);
            } catch (Exception e) {
                log.error("{}相似度计算失败", type, e);
                throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR,
                        type + "相似度计算失败: " + e.getMessage());
            }
        }, duplicateCheckThreadPool);
    }

    private CompletableFuture<BigDecimal> calculateDocumentSimilarity(
            String type, String doc1, String doc2, String fileType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                double similarity = SimHashUtil.calculateGroupSimilarity(doc1, doc2, fileType);
                return BigDecimal.valueOf(similarity);
            } catch (Exception e) {
                log.error("{}文档相似度计算失败", type, e);
                throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR,
                        type + "文档相似度计算失败: " + e.getMessage());
            }
        }, duplicateCheckThreadPool);
    }

    private CompletableFuture<BigDecimal> calculateSourceCodeSimilarity(String source1, String source2) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                double similarity = CodeSimilarityUtil.calculateCodeSimilarity(source1, source2);
                return BigDecimal.valueOf(similarity);
            } catch (Exception e) {
                log.error("源代码相似度计算失败", e);
                throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR,
                        "源代码相似度计算失败: " + e.getMessage());
            }
        }, duplicateCheckThreadPool);
    }

    /**
     * 计算总相似度
     * 各部分权重：
     * - 简介、背景各占10%
     * - 系统设计、Word文档、PDF文档、源代码各占20%
     */
    private BigDecimal calculateTotalSimilarity(DuplicateCheck duplicateCheck) {
        try {
            BigDecimal pct1 = new BigDecimal("0.1"); // 10%权重
            BigDecimal pct2 = new BigDecimal("0.2"); // 20%权重

            return duplicateCheck.getBriefValue().multiply(pct1)
                    .add(duplicateCheck.getBackgroundValue().multiply(pct1))
                    .add(duplicateCheck.getSystemDesignValue().multiply(pct2))
                    .add(duplicateCheck.getWordValue().multiply(pct2))
                    .add(duplicateCheck.getPdfValue().multiply(pct2))
                    .add(duplicateCheck.getSourceValue().multiply(pct2));
        } catch (Exception e) {
            log.error("计算总相似度失败", e);
            throw new BusinessException(ErrorCode.DUPLICATE_CHECK_ERROR, "计算总相似度失败: " + e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        if (duplicateCheckThreadPool != null) {
            duplicateCheckThreadPool.shutdown();
            try {
                if (!duplicateCheckThreadPool.awaitTermination(1200, TimeUnit.SECONDS)) {
                    duplicateCheckThreadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                duplicateCheckThreadPool.shutdownNow();
            }
        }
    }
}