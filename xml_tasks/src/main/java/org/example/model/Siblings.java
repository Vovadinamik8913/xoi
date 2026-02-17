package org.example.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Siblings {
    @XmlElement(name = "brotherRef")
    private List<Ref> brotherRef = new ArrayList<>();
    @XmlElement(name = "sisterRef")
    private List<Ref> sisterRef = new ArrayList<>();
    @XmlElement(name = "siblingRef")
    private List<Ref> siblingRef = new ArrayList<>();

    @XmlElement(name = "brotherName")
    private List<String> brotherName = new ArrayList<>();
    @XmlElement(name = "sisterName")
    private List<String> sisterName = new ArrayList<>();
    @XmlElement(name = "siblingName")
    private List<String> siblingName = new ArrayList<>();
}
