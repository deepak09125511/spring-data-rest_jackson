/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.rest.webmvc.config;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.keyvalue.core.mapping.context.KeyValueMappingContext;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.querydsl.binding.QuerydslPredicateBuilder;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.webmvc.json.MappedJacksonProperties;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


class QuerydslAwareRootResourceInformationHandlerMethodArgumentResolverUnitTests {
    QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver resolver;
    MappedJacksonProperties orderProperties;
    MappedJacksonProperties customerProperties;
    MappedJacksonProperties itemProperties;
    QuerydslPredicateBuilder predicateBuilder;
    QuerydslBindingsFactory factory;
    @BeforeEach
    void setUp() {

        KeyValueMappingContext<?, ?> context = new KeyValueMappingContext<>();

        JsonMapper mapper = JsonMapper.builder().build();

        PersistentEntity<?, ?> orderEntity =
                context.getRequiredPersistentEntity(Order.class);

        orderProperties =
                MappedJacksonProperties.forSerialization(orderEntity, mapper);

        PersistentEntity<?, ?> customerEntity =
                context.getRequiredPersistentEntity(Customer.class);

        customerProperties =
                MappedJacksonProperties.forSerialization(customerEntity, mapper);
        PersistentEntity<?, ?> itemEntity =
                context.getRequiredPersistentEntity(Item.class);

        itemProperties =
                MappedJacksonProperties.forSerialization(itemEntity, mapper);

        predicateBuilder = mock(QuerydslPredicateBuilder.class);
        factory = mock(QuerydslBindingsFactory.class);

        resolver = new QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver(
                mock(Repositories.class),
                mock(RepositoryInvokerFactory.class),
                mock(ResourceMetadataHandlerMethodArgumentResolver.class),
                predicateBuilder,
                factory,
                type -> {
                    if (type.equals(Order.class)) {
                        return orderProperties;
                    }
                    if (type.equals(Customer.class)) {
                        return customerProperties;
                    }
                    if (type.equals(Item.class)) {
                        return itemProperties;
                    }
                    return null;
                });
    }
    static class Order {

        private Customer customer;

        public Customer getCustomer() {
            return customer;
        }
        private List<Item> items;

        public List<Item> getItems() {
            return items;
        }
    }
    static class Item {
        private String name;
        public String getName() {
            return name;
        }
    }

    static class Customer {

        private String firstname;
        private String password;

        public String getFirstname() {
            return firstname;
        }

        @JsonIgnore
        public String getPassword() {
            return password;
        }
    }

    @Test
    void rejectsNestedPropertyHiddenByJackson() throws Exception {

        Map<String, String[]> parameters = Map.of(
                "customer.password", new String[] { "secret123" });

        Map<String, String[]> filtered = filterParameters(parameters);

        assertThat(filtered).doesNotContainKey("customer.password");
    }
    @Test
    void testQuerydslNestedProperty() {

        EntityPathResolver entityPathResolver = new EntityPathResolver() {

            @Override
            @SuppressWarnings("unchecked")
            public <T> EntityPath<T> createPath(Class<T> domainType) {

                if (domainType.equals(Order.class)) {
                    return (EntityPath<T>) new QOrder("order");
                }

                throw new IllegalArgumentException(
                        "Unknown type: " + domainType);
            }
        };

        QuerydslBindingsFactory factory =
                new QuerydslBindingsFactory(entityPathResolver);

        QuerydslPredicateBuilder predicateBuilder =
                new QuerydslPredicateBuilder(
                        DefaultConversionService.getSharedInstance(),
                        entityPathResolver);

        TypeInformation<Order> type = TypeInformation.of(Order.class);

        QuerydslBindings bindings = factory.createBindingsFor(type);

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();

        parameters.add("customer.firstname", "Deepak");

        Predicate predicate = predicateBuilder.getPredicate(
                type, parameters, bindings);
        assertThat(predicate).isNotNull();

    }

    @Test
    void rejectsUnknownNestedProperty() throws Exception {

        Map<String, String[]> parameters = Map.of(
                "customer.unknown", new String[] { "Deepak" });

        Map<String, String[]> filtered = filterParameters(parameters);

        assertThat(filtered).doesNotContainKey("customer.unknown");
    }
    @Test
    void rejectsUnknownRootProperty() throws Exception {

        Map<String, String[]> parameters = Map.of(
                "unknown.firstname", new String[] { "Deepak" });

        Map<String, String[]> filtered = filterParameters(parameters);

        assertThat(filtered).doesNotContainKey("unknown.firstname");
    }
    @Test
    void retainsDirectProperty() throws Exception {

        Map<String, String[]> parameters = Map.of(
                "customer", new String[] { "Deepak" });

        Map<String, String[]> filtered = filterParameters(parameters);

        assertThat(filtered).containsKey("customer");
    }
    @Test
    void retainsVisibleNestedCollectionProperty() throws Exception {

        Map<String, String[]> parameters = Map.of(
                "items.name", new String[] { "Laptop" });

        Map<String, String[]> filtered = filterParameters(parameters);

        assertThat(filtered).containsKey("items.name");

        assertThat(filtered.get("items.name"))
                .containsExactly("Laptop");
    }
    @Test
    void rejectsUnknownNestedCollectionProperty() throws Exception {

        Map<String, String[]> parameters = Map.of(
                "items.unknown", new String[] { "Laptop" });

        Map<String, String[]> filtered = filterParameters(parameters);

        assertThat(filtered).doesNotContainKey("items.unknown");
    }
    @Test
    void passesVisibleNestedPropertyToQuerydsl() throws Exception {

        QuerydslBindings bindings = new QuerydslBindings();

        when(factory.createBindingsFor(TypeInformation.of(Order.class)))
                .thenReturn(bindings);

        when(predicateBuilder.getPredicate(
                eq(TypeInformation.of(Order.class)),
                any(),
                eq(bindings)))
                .thenReturn(mock(Predicate.class));

        QuerydslPredicateExecutor<?> repository =
                mock(QuerydslPredicateExecutor.class);

        Map<String, String[]> parameters = Map.of(
                "customer.firstname", new String[] { "Deepak" });

        Method method =
                QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver.class
                        .getDeclaredMethod(
                                "getRepositoryAndPredicate",
                                QuerydslPredicateExecutor.class,
                                Class.class,
                                Map.class);

        method.setAccessible(true);

        method.invoke(resolver, repository, Order.class, parameters);

        verify(predicateBuilder).getPredicate(
                eq(TypeInformation.of(Order.class)),
                argThat(map -> map.containsKey("customer.firstname")),
                eq(bindings));
    }
    @Test
    void doesNotPassJacksonHiddenNestedPropertyToQuerydsl() throws Exception {
        QuerydslBindings bindings = new QuerydslBindings();

        when(factory.createBindingsFor(TypeInformation.of(Order.class)))
                .thenReturn(bindings);

        QuerydslPredicateExecutor<?> repository =
                mock(QuerydslPredicateExecutor.class);

        Map<String, String[]> parameters = Map.of(
                "customer.password", new String[] { "secret123" });

        Method method =
                QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver.class
                        .getDeclaredMethod(
                                "getRepositoryAndPredicate",
                                QuerydslPredicateExecutor.class,
                                Class.class,
                                Map.class);

        method.setAccessible(true);

        method.invoke(resolver, repository, Order.class, parameters);

        verify(predicateBuilder).getPredicate(
                eq(TypeInformation.of(Order.class)),
                argThat(map -> !map.containsKey("customer.password")),
                eq(bindings));
    }
    @SuppressWarnings("unchecked")
    private Map<String, String[]> filterParameters(
            Map<String, String[]> parameters) throws Exception {

        Method method =
                QuerydslAwareRootResourceInformationHandlerMethodArgumentResolver.class
                        .getDeclaredMethod(
                                "filterByJacksonVisibility", Class.class, Map.class);

        method.setAccessible(true);

        return (Map<String, String[]>) method.invoke(
                resolver, Order.class, parameters);
    }
}