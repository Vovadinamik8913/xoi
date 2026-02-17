package org.example.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.Data;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Expected {
    @XmlAttribute(name = "children")
    private Integer children;
    @XmlAttribute(name = "siblings")
    private Integer siblings;
}

