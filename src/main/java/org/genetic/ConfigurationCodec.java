package org.genetic;

import io.jenetics.Genotype;
import io.jenetics.LongGene;
import io.jenetics.engine.Codec;
import io.jenetics.util.Factory;
import java.util.function.Function;
import org.owl.parser.configuration.FunctionalConfiguration;

public class ConfigurationCodec implements Codec<FunctionalConfiguration, LongGene> {

  @Override
  public Factory<Genotype<LongGene>> encoding() {
    return null;
  }

  @Override
  public Function<Genotype<LongGene>, FunctionalConfiguration> decoder() {
    return null;
  }
}
