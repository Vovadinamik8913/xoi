package org.example.model;

import jakarta.xml.bind.annotation.*;
import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Person {
    @XmlAttribute(name = "id")
    @XmlID
    private String id;

    private Name name;

    @XmlElement(required = true)
    private Gender gender = Gender.UNKNOWN;

    private Spouses spouses;
    private Parents parents;
    private Children children;
    private Siblings siblings;
    private Expected expected;
    private Family family;
}
