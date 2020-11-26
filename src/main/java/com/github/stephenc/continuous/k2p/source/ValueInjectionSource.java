package com.github.stephenc.continuous.k2p.source;

public class ValueInjectionSource implements InjectionSource {
    private final String value;

    public ValueInjectionSource(String value) {
        this.value = value;
    }

    @Override
    public void resolve() {

    }

    public String getValue() {
        return value;
    }

    @Override
    public int compareTo(InjectionSource o) {
        if (this == o) {
            return 0;
        }
        if (getClass() != o.getClass()) {
            return o.getClass().getName().compareTo(getClass().getName());
        }
        ValueInjectionSource that = (ValueInjectionSource) o;

        return value.compareTo(that.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ValueInjectionSource that = (ValueInjectionSource) o;

        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
