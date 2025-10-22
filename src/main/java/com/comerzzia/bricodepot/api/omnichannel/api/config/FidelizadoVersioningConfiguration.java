package com.comerzzia.bricodepot.api.omnichannel.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.comerzzia.bricodepot.backoffice.services.fidelizacion.fidelizados.versioning.CustomFidelizadoVersionControlServiceImpl;
import com.comerzzia.servicios.fidelizacion.fidelizados.versioning.FidelizadoVersionControlServiceImpl;

@Configuration
public class FidelizadoVersioningConfiguration {

    @Bean(name = "fidVersionControlService")
    @Primary
    public FidelizadoVersionControlServiceImpl fidVersionControlService() {
        return CustomFidelizadoVersionControlServiceImpl.get();
    }
}
