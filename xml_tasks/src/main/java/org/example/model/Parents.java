package org.example.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@Getter
public class Parents {
    @XmlElement(name = "parentRef")
    private List<Ref> parentRef = new ArrayList<>();
    @XmlElement(name = "parentName")
    private List<String> parentName = new ArrayList<>();
}

