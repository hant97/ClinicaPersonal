package com.clinica.backend.config;

import com.clinica.backend.model.Catalog;
import com.clinica.backend.model.CatalogItem;
import com.clinica.backend.repository.CatalogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Profile("!test")
public class CatalogDataInitializer implements CommandLineRunner {

    @Autowired
    private CatalogRepository catalogRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeCatalogIfNotFound("MARITAL_STATUS", "Estado Civil", "Estados civiles para pacientes", "GENERAL",
            Arrays.asList("Soltero(a)", "Casado(a)", "Viudo(a)", "Divorciado(a)", "Conviviente"));
            
        initializeCatalogIfNotFound("GENDER", "Género", "Identidad de género", "GENERAL",
            Arrays.asList("Masculino", "Femenino", "Otro", "Prefiero no decirlo"));
            
        initializeCatalogIfNotFound("DOCUMENT_TYPE", "Tipo de Documento", "Tipos de documento de identidad", "GENERAL",
            Arrays.asList("DNI", "Pasaporte", "Carné de Extranjería"));
            
        initializeCatalogIfNotFound("APPOINTMENT_MODALITY", "Modalidad de Cita", "Modalidad de atención", "GENERAL",
            Arrays.asList("Presencial", "Virtual"));
            
        initializeCatalogIfNotFound("PAYMENT_METHOD", "Método de Pago", "Formas de pago aceptadas", "GENERAL",
            Arrays.asList("Efectivo", "Transferencia", "Tarjeta de Crédito/Débito", "Yape", "Plin"));
            
        initializeCatalogIfNotFound("SUPPLY_UNIT", "Unidad de Medida", "Unidades para el inventario", "GENERAL",
            Arrays.asList("Unidades", "Cajas", "Paquetes", "Litros", "Mililitros"));
            
        initializeCatalogIfNotFound("RISK_ALERT_TYPE", "Tipo de Alerta de Riesgo", "Tipos de riesgo para pacientes", "PSICOLOGIA",
            Arrays.asList("Ideación Suicida", "Autolesión", "Violencia Familiar", "Abuso de Sustancias", "Riesgo de Fuga", "Deserción del Tratamiento", "Otro"));
            
        initializeCatalogIfNotFound("RISK_ALERT_LEVEL", "Nivel de Riesgo", "Niveles de gravedad de riesgo", "GENERAL",
            Arrays.asList("Bajo", "Moderado", "Alto", "Crítico"));
    }

    private void initializeCatalogIfNotFound(String code, String name, String description, String specialty, List<String> itemNames) {
        var existingOpt = catalogRepository.findByCode(code);
        if (existingOpt.isEmpty()) {
            Catalog catalog = new Catalog();
            catalog.setCode(code);
            catalog.setName(name);
            catalog.setDescription(description);
            catalog.setSpecialty(specialty);

            int index = 0;
            for (String itemName : itemNames) {
                CatalogItem item = new CatalogItem();
                item.setCatalog(catalog);
                item.setItemCode(itemName.toUpperCase().replace(" ", "_").replace("(", "").replace(")", "").replace("/", "_"));
                item.setItemName(itemName);
                item.setActive(true);
                item.setOrderIndex(index++);
                catalog.getItems().add(item);
            }

            catalogRepository.save(catalog);
        } else {
            Catalog existing = existingOpt.get();
            // Si el catálogo transversal estaba marcado como PSICOLOGIA, actualizar a GENERAL
            if ("GENERAL".equals(specialty) && "PSICOLOGIA".equals(existing.getSpecialty())) {
                existing.setSpecialty("GENERAL");
                catalogRepository.save(existing);
            }
        }
    }
}
