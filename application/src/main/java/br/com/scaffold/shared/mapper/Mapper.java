package br.com.scaffold.shared.mapper;

@FunctionalInterface
public interface Mapper<In, Out> {
    Out map(In input);
}
