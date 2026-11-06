package org.springframework.data.rest.webmvc.config;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.StringPath;

public class QCustomer extends EntityPathBase<
        QuerydslAwareRootResourceInformationHandlerMethodArgumentResolverUnitTests.Customer> {

    public final StringPath firstname = createString("firstname");

    public QCustomer(PathMetadata metadata) {
        super(QuerydslAwareRootResourceInformationHandlerMethodArgumentResolverUnitTests.Customer.class,
                metadata);
    }

    public QCustomer(String variable) {
        super(QuerydslAwareRootResourceInformationHandlerMethodArgumentResolverUnitTests.Customer.class,
                variable);
    }
}
