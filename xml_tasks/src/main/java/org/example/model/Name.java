package org.example.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Name {
    private String first;
    private String last;
    private String full;
}
