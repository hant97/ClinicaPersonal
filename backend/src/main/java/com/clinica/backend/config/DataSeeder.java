package com.clinica.backend.config;

import com.clinica.backend.model.PsychometricTest;
import com.clinica.backend.repository.PsychometricTestRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(PsychometricTestRepository testRepository) {
        return args -> {
            if (testRepository.count() == 0) {
                PsychometricTest bdiTest = new PsychometricTest();
                bdiTest.setName("Inventario de Depresión de Beck (BDI-II)");
                bdiTest.setDescription("Cuestionario de 21 ítems que evalúa la severidad de la depresión.");
                bdiTest.setQuestionsJson("[\n" +
                        "      {\n" +
                        "        \"id\": 1,\n" +
                        "        \"text\": \"Tristeza\",\n" +
                        "        \"options\": [\n" +
                        "          {\"score\": 0, \"text\": \"No me siento triste.\"},\n" +
                        "          {\"score\": 1, \"text\": \"Me siento triste gran parte del tiempo.\"},\n" +
                        "          {\"score\": 2, \"text\": \"Estoy triste todo el tiempo.\"},\n" +
                        "          {\"score\": 3, \"text\": \"Estoy tan triste o soy tan infeliz que no puedo soportarlo.\"}\n" +
                        "        ]\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"id\": 2,\n" +
                        "        \"text\": \"Pesimismo\",\n" +
                        "        \"options\": [\n" +
                        "          {\"score\": 0, \"text\": \"No estoy desanimado respecto del mi futuro.\"},\n" +
                        "          {\"score\": 1, \"text\": \"Me siento más desanimado respecto de mi futuro que lo que solía estarlo.\"},\n" +
                        "          {\"score\": 2, \"text\": \"No espero que las cosas funcionen a mi favor.\"},\n" +
                        "          {\"score\": 3, \"text\": \"Siento que no hay esperanza para mi futuro y que las cosas solo empeorarán.\"}\n" +
                        "        ]\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"id\": 3,\n" +
                        "        \"text\": \"Fracasos pasados\",\n" +
                        "        \"options\": [\n" +
                        "          {\"score\": 0, \"text\": \"No me siento como un fracasado.\"},\n" +
                        "          {\"score\": 1, \"text\": \"He fracasado más de lo que hubiera debido.\"},\n" +
                        "          {\"score\": 2, \"text\": \"Cuando miro hacia atrás, veo muchos fracasos.\"},\n" +
                        "          {\"score\": 3, \"text\": \"Siento que como persona soy un fracaso total.\"}\n" +
                        "        ]\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"id\": 4,\n" +
                        "        \"text\": \"Pérdida de placer\",\n" +
                        "        \"options\": [\n" +
                        "          {\"score\": 0, \"text\": \"Obtengo tanto placer como siempre de las cosas de las que disfruto.\"},\n" +
                        "          {\"score\": 1, \"text\": \"No disfruto tanto de las cosas como solía hacerlo.\"},\n" +
                        "          {\"score\": 2, \"text\": \"Obtengo muy poco placer de las cosas que solía disfrutar.\"},\n" +
                        "          {\"score\": 3, \"text\": \"No puedo obtener ningún placer de las cosas de las que solía disfrutar.\"}\n" +
                        "        ]\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"id\": 5,\n" +
                        "        \"text\": \"Sentimiento de culpa\",\n" +
                        "        \"options\": [\n" +
                        "          {\"score\": 0, \"text\": \"No me siento particularmente culpable.\"},\n" +
                        "          {\"score\": 1, \"text\": \"Me siento culpable respecto de varias cosas que he hecho o que debería haber hecho.\"},\n" +
                        "          {\"score\": 2, \"text\": \"Me siento bastante culpable la mayor parte del tiempo.\"},\n" +
                        "          {\"score\": 3, \"text\": \"Me siento culpable todo el tiempo.\"}\n" +
                        "        ]\n" +
                        "      }\n" +
                        "    ]");
                testRepository.save(bdiTest);
            }
        };
    }
}
