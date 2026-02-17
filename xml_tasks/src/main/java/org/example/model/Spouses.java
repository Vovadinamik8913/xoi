package org.example.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Spouses {
    @XmlElement(name = "spouseRef")
    private List<Ref> spouseRef = new ArrayList<>();
    @XmlElement(name = "spouseName")
    private List<String> spouseName = new ArrayList<>();
}
