package ru.rt.rostelecom_tms.service.embedding;

import java.util.List;

public interface EmbeddingProviderClient {

    EmbeddingProvider provider();

    List<Double> embed(String text);
}
