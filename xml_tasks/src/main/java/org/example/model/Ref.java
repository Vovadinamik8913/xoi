package org.example.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.AllArgsConstructor;
import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
@AllArgsConstructor
public class Ref {
    @XmlAttribute(name = "ref", required = true)
    private String ref;
}

