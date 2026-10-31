package edu.itc.se.shop.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final ObjectProvider<ReportBuilder> builders;

    public ReportService(ObjectProvider<ReportBuilder> builders) {
        this.builders = builders;
    }

    public void demo() {
        ReportBuilder a = builders.getObject();
        ReportBuilder b = builders.getObject();
        System.out.println("a=" + System.identityHashCode(a)
                + " b=" + System.identityHashCode(b)
                + " same=" + (a == b));
    }
}