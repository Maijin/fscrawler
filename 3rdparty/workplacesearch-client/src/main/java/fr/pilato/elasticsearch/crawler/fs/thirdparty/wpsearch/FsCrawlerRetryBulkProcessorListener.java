/*
 * Licensed to David Pilato (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.pilato.elasticsearch.crawler.fs.thirdparty.wpsearch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This Listener implements a simple and naive retry mechanism. When a document is rejected because of a es_rejected_execution_exception
 * the same document is sent again to the bulk processor.
 */
public class FsCrawlerRetryBulkProcessorListener<
        O extends FsCrawlerOperation<O>,
        Req extends FsCrawlerBulkRequest<O>,
        Res extends FsCrawlerBulkResponse<O>
        > extends FsCrawlerAdvancedBulkProcessorListener<O, Req, Res> {

    private static final Logger logger = LogManager.getLogger(FsCrawlerRetryBulkProcessorListener.class);

    @Override
    public void afterBulk(long executionId, Req request, Res response) {
        super.afterBulk(executionId, request, response);
        if (response.hasFailures()) {
            for (Res.BulkItemResponse<O> item : response.getItems()) {
                if (item.isFailed() && item.getFailureMessage().contains("es_rejected_execution_exception")) {
                    logger.debug("We are going to retry document [{}] because of [{}]",
                            item.getOperation(), item.getFailureMessage());
                    // Find request
                    boolean requestFound = false;
                    for (O operation : request.getOperations()) {
                        if (operation.compareTo(item.getOperation()) == 0) {
                            this.bulkProcessor.add(operation);
                            requestFound = true;
                            logger.debug("Document [{}] found. Can be retried.", item.getOperation());
                            break;
                        }
                    }
                    if (!requestFound) {
                        logger.warn("Can not retry document [{}] because we can't find it anymore.",
                                item.getOperation());
                    }
                }
            }
        }
    }
}