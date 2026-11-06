package org.springframework.data.rest.webmvc.config;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.SimplePath;

public class QOrder extends EntityPathBase<QuerydslAwareRootResourceInformationHandlerMethodArgumentResolverUnitTests.Order> {

    public final QCustomer customer =
            new QCustomer(forProperty("customer"));

    public QOrder(String variable) {
        super(QuerydslAwareRootResourceInformationHandlerMethodArgumentResolverUnitTests.Order.class, variable);
    }
}
