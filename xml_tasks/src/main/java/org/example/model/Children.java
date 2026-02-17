package org.example.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class Children {
    @XmlElement(name = "sonRef")
    private List<Ref> sonRef = new ArrayList<>();
    @XmlElement(name = "daughterRef")
    private List<Ref> daughterRef = new ArrayList<>();
    @XmlElement(name = "childRef")
    private List<Ref> childRef = new ArrayList<>();

    @XmlElement(name = "sonName")
    private List<String> sonName = new ArrayList<>();
    @XmlElement(name = "daughterName")
    private List<String> daughterName = new ArrayList<>();
    @XmlElement(name = "childName")
    private List<String> childName = new ArrayList<>();
}

