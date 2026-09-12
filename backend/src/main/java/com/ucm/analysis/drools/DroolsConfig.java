package com.ucm.analysis.drools;

import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.io.Resource;
import org.kie.api.runtime.KieContainer;
import org.kie.internal.io.ResourceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Drools 规则引擎配置：启动时编译 classpath 下的整改推荐规则 */
@Configuration
public class DroolsConfig {

    @Bean
    public KieContainer kieContainer() {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        Resource resource = ResourceFactory.newClassPathResource("drools/rectification.drl");
        kfs.write(resource);
        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            throw new IllegalStateException("整改推荐规则编译失败: " + kb.getResults().getMessages());
        }
        return ks.newKieContainer(kb.getKieModule().getReleaseId());
    }
}
