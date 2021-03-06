package com.googlecode.simpleobjectassembler.converter.mapping;

import java.util.HashSet;
import java.util.Set;


public class Exclusions {

   private final Set<String> properties = new HashSet<String>();

   public Exclusions() {

   }

   public Exclusions(String... properties) {
      for (String property : properties) {
         this.properties.add(property);
      }
   }

   public Exclusions(Set properties) {
      this.properties.addAll(properties);
   }

   public Exclusions add(String property) {
      this.properties.add(property);
      return this;
   }

   public Exclusions remove(String property) {
      this.properties.remove(property);
      return this;
   }

   public Exclusions empty() {
      this.properties.clear();
      return this;
   }

   public Set<String> getSet() {
      return this.properties;
   }

   public boolean contains(String value) {
      return this.properties.contains(value);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Exclusions that = (Exclusions) o;

      if (properties != null ? !properties.equals(that.properties) : that.properties != null) return false;

      return true;
   }

   @Override
   public int hashCode() {
      return properties != null ? properties.hashCode() : 0;
   }
}
