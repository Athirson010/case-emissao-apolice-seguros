package io.github.athirson010.application.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Insurance Policy Proposal API")
                        .version("1.0.0")
                        .description("""
                                Sistema robusto e escalável para emissão, gerenciamento e consulta de propostas de apólices de seguros.
                                
                                Desenvolvido com foco em **Arquitetura Hexagonal (Ports and Adapters)** e boas práticas de desenvolvimento.
                                
                                ### Funcionalidades Principais
                                - Criar nova proposta de apólice
                                - Consultar proposta por ID
                                - Cancelar proposta de apólice
                                - Máquina de estados com transições validadas
                                - Histórico completo de alterações de status
                                
                                ### Categorias Suportadas
                                - **AUTO** - Seguro Automotivo
                                - **VIDA** - Seguro de Vida
                                - **RESIDENCIAL** - Seguro Residencial
                                - **OUTROS** - Outros tipos de seguro
                                """)
                        .contact(new Contact()
                                .name("Athirson de Oliveira")
                                .email("athirson.candido@bandtec.com.br")
                                .url("https://br.linkedin.com/in/athirson-oliveira"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor de Desenvolvimento")
                ));
    }
}
