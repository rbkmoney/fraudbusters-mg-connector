package com.rbkmoney.fraudbusters.mg.connector.converter;

public interface BinaryConverter<T> {

    T convert(byte[] bin, Class<T> clazz);

}
