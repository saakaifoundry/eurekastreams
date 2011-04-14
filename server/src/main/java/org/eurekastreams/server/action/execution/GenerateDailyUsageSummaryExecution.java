/*
 * Copyright (c) 2010-2011 Lockheed Martin Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eurekastreams.server.action.execution;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.eurekastreams.commons.actions.ExecutionStrategy;
import org.eurekastreams.commons.actions.context.ActionContext;
import org.eurekastreams.commons.exceptions.ExecutionException;
import org.eurekastreams.commons.logging.LogFactory;
import org.eurekastreams.server.domain.DailyUsageSummary;
import org.eurekastreams.server.persistence.mappers.DomainMapper;
import org.eurekastreams.server.persistence.mappers.requests.PersistenceRequest;
import org.eurekastreams.server.persistence.strategies.GetDateFromDaysAgoStrategy;

/**
 * Execution strategy to generate the daily usage summary for the previous day.
 */
public class GenerateDailyUsageSummaryExecution implements ExecutionStrategy<ActionContext>
{
    /**
     * Logger.
     */
    private Log logger = LogFactory.make();

    /**
     * strategy to get a date from N days ago.
     */
    private GetDateFromDaysAgoStrategy daysAgoDateStrategy;

    /**
     * Mapper to get a single day's DailyUsageSummary.
     */
    private DomainMapper<Date, DailyUsageSummary> getDailyUsageSummaryByDateMapper;

    /**
     * Mapper to get a day's message count.
     */
    private DomainMapper<Date, Long> getDailyMessageCountMapper;

    /**
     * Mapper to get a day's page view count.
     */
    private DomainMapper<Date, Long> getDailyPageViewCountMapper;

    /**
     * Mapper to get a day's stream contributor count.
     */
    private DomainMapper<Date, Long> getDailyStreamContributorCountMapper;

    /**
     * Mapper to get a day's stream view count.
     */
    private DomainMapper<Date, Long> getDailyStreamViewCountMapper;

    /**
     * Mapper to get a day's stream viewer count.
     */
    private DomainMapper<Date, Long> getDailyStreamViewerCountMapper;

    /**
     * Mapper to get a day's unique visitor count.
     */
    private DomainMapper<Date, Long> getDailyUniqueVisitorCountMapper;

    /**
     * Mapper to insert the DailyUsageSummary entity.
     */
    private DomainMapper<PersistenceRequest<DailyUsageSummary>, Boolean> insertMapper;

    /**
     * Mapper to delete old UsageMetric data.
     */
    private DomainMapper<Serializable, Serializable> usageMetricDataCleanupMapper;

    /**
     * Mapper to get day's average activity response time (for those that had responses).
     */
    private DomainMapper<Date, Long> getDailyMessageResponseTimeMapper;

    /**
     * Constructor.
     * 
     * @param inDaysAgoDateStrategy
     *            strategy to get a date from yesterday
     * @param inGetDailyUsageSummaryByDateMapper
     *            Mapper to get a single day's DailyUsageSummary
     * @param inGetDailyMessageCountMapper
     *            Mapper to get a day's message count
     * @param inGetDailyPageViewCountMapper
     *            Mapper to get a day's page view count.
     * @param inGetDailyStreamContributorCountMapper
     *            Mapper to get a day's stream contributor count.
     * @param inGetDailyStreamViewCountMapper
     *            Mapper to get a day's stream view count.
     * @param inGetDailyStreamViewerCountMapper
     *            Mapper to get a day's stream viewer count.
     * @param inGetDailyUniqueVisitorCountMapper
     *            Mapper to get a day's unique visitor count.
     * @param inGetDailyMessageResponseTimeMapper
     *            Mapper to get day's average activity response time (for those that had responses).
     * @param inInsertMapper
     *            mapper to insert DailyUsageSummary
     * @param inUsageMetricDataCleanupMapper
     *            mapper to delete old UsageMetric data
     */
    public GenerateDailyUsageSummaryExecution(final GetDateFromDaysAgoStrategy inDaysAgoDateStrategy,
            final DomainMapper<Date, DailyUsageSummary> inGetDailyUsageSummaryByDateMapper,
            final DomainMapper<Date, Long> inGetDailyMessageCountMapper,
            final DomainMapper<Date, Long> inGetDailyPageViewCountMapper,
            final DomainMapper<Date, Long> inGetDailyStreamContributorCountMapper,
            final DomainMapper<Date, Long> inGetDailyStreamViewCountMapper,
            final DomainMapper<Date, Long> inGetDailyStreamViewerCountMapper,
            final DomainMapper<Date, Long> inGetDailyUniqueVisitorCountMapper,
            final DomainMapper<Date, Long> inGetDailyMessageResponseTimeMapper,
            final DomainMapper<PersistenceRequest<DailyUsageSummary>, Boolean> inInsertMapper,
            final DomainMapper<Serializable, Serializable> inUsageMetricDataCleanupMapper)
    {
        daysAgoDateStrategy = inDaysAgoDateStrategy;
        getDailyUsageSummaryByDateMapper = inGetDailyUsageSummaryByDateMapper;
        getDailyMessageCountMapper = inGetDailyMessageCountMapper;
        getDailyPageViewCountMapper = inGetDailyPageViewCountMapper;
        getDailyStreamContributorCountMapper = inGetDailyStreamContributorCountMapper;
        getDailyStreamViewCountMapper = inGetDailyStreamViewCountMapper;
        getDailyStreamViewerCountMapper = inGetDailyStreamViewerCountMapper;
        getDailyUniqueVisitorCountMapper = inGetDailyUniqueVisitorCountMapper;
        getDailyMessageResponseTimeMapper = inGetDailyMessageResponseTimeMapper;
        insertMapper = inInsertMapper;
        usageMetricDataCleanupMapper = inUsageMetricDataCleanupMapper;
    }

    /**
     * Generate the daily usage summary for the previous day.
     * 
     * @param inActionContext
     *            the action context
     * @return true if data was inserted, false if already existed
     * @throws ExecutionException
     *             when something really, really bad happens
     */
    @Override
    public Serializable execute(final ActionContext inActionContext) throws ExecutionException
    {
        Date yesterday = daysAgoDateStrategy.execute(1);

        // see if we already have data for yesterday
        DailyUsageSummary data = getDailyUsageSummaryByDateMapper.execute(yesterday);
        if (data != null)
        {
            logger.info("No need to create daily usage data for " + yesterday + " - already exists.");
            return Boolean.FALSE;
        }

        long uniqueVisitorCount = getDailyUniqueVisitorCountMapper.execute(yesterday);
        long pageViewCount = getDailyPageViewCountMapper.execute(yesterday);
        long streamViewerCount = getDailyStreamViewerCountMapper.execute(yesterday);
        long streamViewCount = getDailyStreamViewCountMapper.execute(yesterday);
        long streamContributorCount = getDailyStreamContributorCountMapper.execute(yesterday);
        long messageCount = getDailyMessageCountMapper.execute(yesterday);
        long avgActvityResponeTime = getDailyMessageResponseTimeMapper.execute(yesterday);

        data = new DailyUsageSummary(uniqueVisitorCount, pageViewCount, streamViewerCount, streamViewCount,
                streamContributorCount, messageCount, avgActvityResponeTime, yesterday);

        // store this
        insertMapper.execute(new PersistenceRequest<DailyUsageSummary>(data));

        // delete old data
        usageMetricDataCleanupMapper.execute(0);

        logger.info("Inserted Daily Summary metrics for " + yesterday);
        return Boolean.TRUE;
    }
}
