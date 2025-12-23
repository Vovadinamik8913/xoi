package org.example.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public class Person {
    private String id;

    private final Set<String> firstNames = new LinkedHashSet<>();
    private final Set<String> lastNames = new LinkedHashSet<>();
    private final Set<String> fullNames = new LinkedHashSet<>();

    private final Set<Gender> genderEvidence = new LinkedHashSet<>();

    private final Set<String> spouseIds = new LinkedHashSet<>();
    private final Set<String> spouseNames = new LinkedHashSet<>();

    private final Set<String> parentIds = new LinkedHashSet<>();
    private final Set<String> parentNames = new LinkedHashSet<>();

    private final Set<String> siblingIds = new LinkedHashSet<>();
    private final Set<String> siblingNames = new LinkedHashSet<>();
    private final Set<String> brotherNames = new LinkedHashSet<>();
    private final Set<String> sisterNames = new LinkedHashSet<>();

    private final Set<String> sonIds = new LinkedHashSet<>();
    private final Set<String> daughterIds = new LinkedHashSet<>();
    private final Set<String> childIds = new LinkedHashSet<>();
    private final Set<String> sonNames = new LinkedHashSet<>();
    private final Set<String> daughterNames = new LinkedHashSet<>();
    private final Set<String> childNames = new LinkedHashSet<>();

    // explicit parent names by role (when present in source)
    private final Set<String> fatherNames = new LinkedHashSet<>();
    private final Set<String> motherNames = new LinkedHashSet<>();

    @Setter
    private Integer expectedChildren;
    @Setter
    private Integer expectedSiblings;

    public void setId(String v) {
        if (v == null || v.isBlank()) return;
        String nv = v.trim();
        if (this.id == null) this.id = nv;
    }

    public String bestFirst() { return pickBest(firstNames); }
    public String bestLast()  { return pickBest(lastNames); }
    public String bestFull()  {
        String full = pickBest(fullNames);
        if (full != null) return full;
        String fn = bestFirst();
        String ln = bestLast();
        if (fn == null && ln == null) return null;
        if (fn == null) return ln;
        if (ln == null) return fn;
        return fn + " " + ln;
    }

    public Gender resolvedGender() {
        if (genderEvidence.contains(Gender.MALE) && !genderEvidence.contains(Gender.FEMALE)) return Gender.MALE;
        if (genderEvidence.contains(Gender.FEMALE) && !genderEvidence.contains(Gender.MALE)) return Gender.FEMALE;
        if (genderEvidence.isEmpty()) return Gender.UNKNOWN;
        return Gender.UNKNOWN; // conflicting -> unknown
    }

    private String pickBest(Set<String> variants) {
        if (variants.isEmpty()) return null;
        return variants.stream().max(Comparator.comparingInt(String::length)).orElse(null);
    }
}
