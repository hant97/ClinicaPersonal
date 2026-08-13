package com.clinica.backend.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PublicLandingDto {
    private General general = new General();
    private Hero hero = new Hero();
    private Approach approach = new Approach();
    private Contact contact = new Contact();
    private Social social = new Social();
    private Seo seo = new Seo();
    private List<Specialty> specialties = new ArrayList<>();
    private List<Benefit> benefits = new ArrayList<>();
    private List<ProcessStep> processSteps = new ArrayList<>();
    private List<Professional> professionals = new ArrayList<>();

    @Data public static class General { private String commercialName; private String tagline; private String description; private String logoUrl; }
    @Data public static class Hero { private String eyebrow; private String title; private String highlight; private String description; private String primaryButtonText; private String secondaryButtonText; private String imageUrl; }
    @Data public static class Approach { private String title; private String highlight; private String description; private String secondaryDescription; private String ctaText; private String imageUrl; }
    @Data public static class Contact { private String heading; private String description; private String phone; private String whatsapp; private String email; private String address; private String hours; private String mapUrl; }
    @Data public static class Social { private String facebook; private String instagram; private String tiktok; private String linkedin; }
    @Data public static class Seo { private String title; private String description; private String siteName; private String imageUrl; }

    @Data public static class Specialty { private String code; private String label; private String title; private String subtitle; private String iconCode; private List<String> services = new ArrayList<>(); }
    @Data public static class Benefit { private String title; private String description; private String iconCode; }
    @Data public static class ProcessStep { private int stepNumber; private String title; private String description; }
    @Data public static class Professional { private String name; private String specialty; private String licenseNumber; private String description; private String experience; private String careAreas; private String photoUrl; }
}
