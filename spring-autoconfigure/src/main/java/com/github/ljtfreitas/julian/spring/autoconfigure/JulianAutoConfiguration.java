/*
 * Copyright (C) 2021 Tiago de Freitas Lima
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.ljtfreitas.julian.spring.autoconfigure;

import com.github.ljtfreitas.julian.Attempt;
import com.github.ljtfreitas.julian.ProxyBuilder;
import com.github.ljtfreitas.julian.ProxyBuilderExtension;
import com.github.ljtfreitas.julian.contract.ContractReader;
import com.github.ljtfreitas.julian.http.DefaultHTTPResponseFailure;
import com.github.ljtfreitas.julian.http.HTTP;
import com.github.ljtfreitas.julian.http.HTTPResponseFailure;
import com.github.ljtfreitas.julian.http.client.DefaultHTTPClient;
import com.github.ljtfreitas.julian.http.client.HTTPClient;
import com.github.ljtfreitas.julian.http.codec.ByteArrayHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.ByteBufferHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.HTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.InputStreamHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.ScalarHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.StringHTTPMessageCodec;
import com.github.ljtfreitas.julian.http.codec.UnprocessableHTTPMessageCodec;
import com.github.ljtfreitas.julian.spi.Plugins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.net.URL;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.github.ljtfreitas.julian.Preconditions.nonNull;
import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toList;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;
import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;
import static org.springframework.beans.factory.support.AbstractBeanDefinition.AUTOWIRE_BY_TYPE;

@AutoConfiguration
@Import(JulianAutoConfiguration.JulianProxiesRegistrar.class)
public class JulianAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JulianAutoConfiguration.class);

    @Bean @Lazy
    @ConditionalOnMissingBean(ByteArrayHTTPMessageCodec.class)
    ByteArrayHTTPMessageCodec byteArrayHTTPMessageCodec() {
        return ByteArrayHTTPMessageCodec.get();
    }

    @Bean @Lazy
    @ConditionalOnMissingBean(ByteBufferHTTPMessageCodec.class)
    ByteBufferHTTPMessageCodec byteBufferHTTPMessageCodec() {
        return ByteBufferHTTPMessageCodec.get();
    }

    @Bean @Lazy
    @ConditionalOnMissingBean(InputStreamHTTPMessageCodec.class)
    InputStreamHTTPMessageCodec inputStreamHTTPMessageCodec() {
        return InputStreamHTTPMessageCodec.get();
    }

    @Bean @Lazy
    @ConditionalOnMissingBean(UnprocessableHTTPMessageCodec.class)
    UnprocessableHTTPMessageCodec unprocessableHTTPMessageCodec() {
        return UnprocessableHTTPMessageCodec.get();
    }

    @Bean @Lazy
    @ConditionalOnMissingBean(ScalarHTTPMessageCodec.class)
    ScalarHTTPMessageCodec scalarHTTPMessageCodec() {
        return ScalarHTTPMessageCodec.get();
    }

    @Bean @Lazy
    @ConditionalOnMissingBean(StringHTTPMessageCodec.class)
    StringHTTPMessageCodec stringHTTPMessageCodec() {
        return StringHTTPMessageCodec.get();
    }

    @Bean @Lazy
    @ConditionalOnMissingBean(HTTPResponseFailure.class)
    HTTPResponseFailure httpResponseFailure() {
        return DefaultHTTPResponseFailure.get();
    }

    @Bean @Lazy
    @ConditionalOnMissingBean(HTTPClient.Specification.class)
    HTTPClient.Specification httpClientSpecification() {
        return new HTTPClient.Specification();
    }

    @Bean @Lazy
    @ConditionalOnMissingBean(HTTPClient.class)
    HTTPClient httpClient(HTTPClient.Specification spec) {
        return new DefaultHTTPClient(spec);
    }

    static class JulianProxiesRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

        private BeanFactory beanFactory;

        @Override
        public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
            BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

            Collection<String> packages = AutoConfigurationPackages.get(nonNull(beanFactory));

            ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                    AnnotationMetadata metadata = beanDefinition.getMetadata();
                    return metadata.isInterface()
                            && !metadata.isAnnotation()
                            && metadata.hasAnnotation(JulianHTTPClient.class.getCanonicalName());
                }
            };
            scanner.addIncludeFilter(new AnnotationTypeFilter(JulianHTTPClient.class, true));
            scanner.addIncludeFilter(((metadataReader, metadataReaderFactory) -> metadataReader.getClassMetadata().isInterface()));

            Collection<JulianProxyCandidate> candidates = packages.stream()
                    .flatMap(packageName -> scanner.findCandidateComponents(packageName).stream())
                    .map(beanDefinition -> JulianProxyCandidate.valueOf(beanDefinition, beanNameGenerator.generateBeanName(beanDefinition, registry), beanFactory))
                    .peek(candidate -> log.info("julian-http-client candidate bean found: {}", candidate.beanClass.getCanonicalName()))
                    .collect(toList());

            candidates.stream()
                    .filter(candidate -> candidate.specBeanClass != null)
                    .flatMap(candidate -> candidate.specBean().stream())
                    .peek(specBean -> log.info("julian-http-client specification bean found: {}", specBean.getBeanClassName()))
                    .forEach(specBean -> registry.registerBeanDefinition(beanNameGenerator.generateBeanName(specBean, registry), specBean));

            candidates.forEach(candidate -> registry.registerBeanDefinition(candidate.beanName, candidate.bean()));
        }

        @Override
        public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
            this.beanFactory = beanFactory;
        }
    }

    static class JulianProxyCandidate {

        @SuppressWarnings("rawtypes")
        private final Class beanClass;
        private final String beanName;
        private final JulianHTTPClient annotation;
        private final Class<? extends JulianHTTPClientSpecification> specBeanClass;
        private final ConfigurableBeanFactory beanFactory;

        JulianProxyCandidate(Class<?> beanClass, String beanName, JulianHTTPClient annotation, ConfigurableBeanFactory beanFactory) {
            this.beanClass = beanClass;
            this.beanName = beanName;
            this.annotation = annotation;
            this.specBeanClass = annotation.spec().equals(JulianHTTPClientSpecification.class) ? null : annotation.spec();
            this.beanFactory = beanFactory;
        }

        BeanDefinition bean() {
            String name = Optional.ofNullable(annotation.name()).filter(not(String::isEmpty)).orElse(beanName);

            JulianCandidateBean julianCandidateBean = new JulianCandidateBean(beanClass, name, annotation.baseURL(), specBeanClass, beanFactory);

            //noinspection unchecked
            return BeanDefinitionBuilder.genericBeanDefinition(beanClass, julianCandidateBean::getObject)
                    .setLazyInit(true)
                    .setPrimary(true)
                    .setAutowireMode(AUTOWIRE_BY_TYPE)
                    .setScope(SCOPE_PROTOTYPE)
                    .getBeanDefinition();
        }

        Optional<BeanDefinition> specBean() {
            return Optional.ofNullable(specBeanClass)
                    .map(c -> BeanDefinitionBuilder.genericBeanDefinition(c)
                            .setLazyInit(true)
                            .setPrimary(true)
                            .setAutowireMode(AUTOWIRE_BY_TYPE)
                            .setScope(SCOPE_SINGLETON)
                            .getBeanDefinition());
        }

        static JulianProxyCandidate valueOf(BeanDefinition beanDefinition, String beanName, BeanFactory beanFactory) {
            String beanClassName = beanDefinition.getBeanClassName();

            Class<?> beanClass = Attempt.run(() -> Class.forName(beanClassName))
                    .prop(e -> new IllegalArgumentException("It's impossible to load " + beanClassName + "."));

            JulianHTTPClient annotation = AnnotationUtils.findAnnotation(beanClass, JulianHTTPClient.class);

            return new JulianProxyCandidate(beanClass, beanName, annotation, (ConfigurableBeanFactory) beanFactory);
        }
    }

    static class JulianCandidateBean implements FactoryBean<Object> {

        private final Class<?> beanClass;
        private final String name;
        private final String baseURL;
        private final Class<? extends JulianHTTPClientSpecification> specBeanClass;
        private final ConfigurableBeanFactory beanFactory;

        public JulianCandidateBean(Class<?> beanClass, String name, String baseURL, Class<? extends JulianHTTPClientSpecification> specBeanClass, ConfigurableBeanFactory beanFactory) {
            this.beanClass = beanClass;
            this.name = name;
            this.baseURL = baseURL;
            this.specBeanClass = specBeanClass;
            this.beanFactory = beanFactory;
        }

        @Override
        public Object getObject() {
            Optional<? extends JulianHTTPClientSpecification> spec = Optional.ofNullable(specBeanClass)
                    .map(beanFactory::getBean);

            ProxyBuilder builder = new ProxyBuilder();

            spec.map(JulianHTTPClientSpecification::httpClientSpec)
                    .ifPresent(builder.http().client().configure()::apply);

            spec.map(JulianHTTPClientSpecification::httpClient)
                    .ifPresent(builder.http().client()::with);

            spec.map(JulianHTTPClientSpecification::interceptors)
                    .ifPresent(builder.http().interceptors()::add);

            spec.map(JulianHTTPClientSpecification::failures)
                    .ifPresent(builder.http().failure()::with);

            spec.map(JulianHTTPClientSpecification::codecs)
                    .ifPresent(builder.codecs()::add);

            spec.map(JulianHTTPClientSpecification::contractExtensions)
                    .filter(not(Collection::isEmpty))
                    .ifPresent(builder.contract().extensions()::apply);

            spec.map(JulianHTTPClientSpecification::contractReader)
                    .ifPresentOrElse(builder.contract()::reader,
                            () -> beanFactory.getBeanProvider(ContractReader.class).ifAvailable(builder.contract()::reader));

            spec.map(JulianHTTPClientSpecification::http)
                    .ifPresentOrElse(builder.http()::with,
                            () -> beanFactory.getBeanProvider(HTTP.class).ifAvailable(builder.http()::with));

            builder.codecs()
                    .add(beanFactory.getBeanProvider(HTTPMessageCodec.class).stream().collect(toList()));

            Optional.ofNullable(beanFactory.resolveEmbeddedValue("${julian-http-client." + name + ".debug:false}"))
                    .filter(not(String::isEmpty))
                    .map(Boolean::valueOf)
                    .ifPresent(builder.http().client().extensions().debug()::enabled);

            URL endpoint = spec.map(JulianHTTPClientSpecification::endpoint)
                    .map(uri -> Attempt.run(uri::toURL).unsafe())
                    .or(() -> Optional.ofNullable(baseURL)
                            .filter(not(String::isEmpty))
                            .or(() -> Optional.ofNullable(name)
                                    .filter(not(String::isEmpty))
                                    .map(name -> "${julian-http-client." + name + ".base-url}"))
                            .map(beanFactory::resolveEmbeddedValue)
                            .filter(not(String::isEmpty))
                            .map(s -> Attempt.run(() -> new URL(s)).unsafe()))
                    .orElse(null);

            Function<ProxyBuilder, ProxyBuilder> extensions = extensions(spec.map(s -> s::customize), extensions());

            return extensions.apply(builder).build(beanClass, endpoint);
        }

        private Stream<ProxyBuilderExtension> extensions() {
            return new Plugins().all(ProxyBuilderExtension.class);
        }

        private Function<ProxyBuilder, ProxyBuilder> extensions(Optional<ProxyBuilderExtension> spec, Stream<ProxyBuilderExtension> extensions) {
            return Stream.concat(spec.stream(), extensions)
                    .reduce(identity(), (f, e) -> f.andThen(e::apply), (a, b) -> b);
        }

        @Override
        public Class<?> getObjectType() {
            return beanClass;
        }

        @Override
        public boolean isSingleton() {
            return true;
        }
    }
}
