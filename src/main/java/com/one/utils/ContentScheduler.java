package com.one.utils;

import com.one.aim.service.BannerService;
import com.one.aim.service.BlogService;
import com.one.aim.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentScheduler {

    private final BlogService blogService;
    private final BannerService bannerService;
    private final PromotionService promotionService;


//     * Process all scheduled content (blogs, banners, promotions)
//     * Runs every 5 minutes: 0 */5 * * * * = At second 0 of every 5th minute

    @Scheduled(cron = "0 */5 * * * *")
    public void processScheduledContent() {
        log.info("Starting scheduled content processing...");

        long startTime = System.currentTimeMillis();

        try {
            // Process blogs
            blogService.publishScheduledBlogs();

            // Process banners
            bannerService.processScheduledBanners();

            // Process promotions
            promotionService.processScheduledPromotions();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Scheduled content processing completed successfully in {} ms", duration);

        } catch (Exception e) {
            log.error("Error processing scheduled content", e);
        }
    }

    /**
     * Alternative: Run at specific time daily (e.g., midnight)
     * Uncomment if you prefer daily processing instead of every 5 minutes
     */
    // @Scheduled(cron = "0 0 0 * * *") // Every day at midnight
    public void processDailyContent() {
        log.info("Starting daily content processing...");
        processScheduledContent();
    }

    /**
     * Process blogs only - runs every 3 minutes
     * Useful if blogs need more frequent checks than banners/promotions
     */
    @Scheduled(cron = "0 */3 * * * *")
    public void processBlogsOnly() {
        try {
            log.debug("Processing scheduled blogs...");
            blogService.publishScheduledBlogs();
        } catch (Exception e) {
            log.error("Error processing scheduled blogs", e);
        }
    }
}
