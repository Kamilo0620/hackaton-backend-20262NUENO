package com.tuckersoft.branchengine.service;

import java.text.Normalizer;

public class DecisionClassifier {

    public record ClassificationResult(String branchType, String handlerUnit, String outcomeCode) {}

    public static ClassificationResult classify(String rawInput) {
        if (rawInput == null) {
            return new ClassificationResult("ENTRADA_CORRUPTA", "Archivo de Errores", "DISCARD_INPUT");
        }

        String texto = Normalizer
                .normalize(rawInput, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();

        String branchType;
        if (!texto.matches(".*[a-z].*")) {
            branchType = "ENTRADA_CORRUPTA";
        } else if (texto.contains("netflix") || texto.contains("camara") || texto.contains("espectador") || texto.contains("videojuego")) {
            branchType = "RUPTURA_CUARTA_PARED";
        } else if (texto.contains("vigilan") || texto.contains("simbolo") || texto.contains("conspiracion")) {
            branchType = "SOSPECHA";
        } else if (texto.contains("rechaza") || texto.contains("destruye") || texto.contains("desobedece") || texto.contains("renuncia")) {
            branchType = "REBELDIA";
        } else {
            branchType = "OBEDIENCIA";
        }

        String handlerUnit;
        String outcomeCode;
        switch (branchType) {
            case "OBEDIENCIA" -> {
                handlerUnit = "Mesa de Guion";
                outcomeCode = "ADVANCE_MAIN_PATH";
            }
            case "REBELDIA" -> {
                handlerUnit = "Control de Continuidad";
                outcomeCode = "FORK_TIMELINE";
            }
            case "SOSPECHA" -> {
                handlerUnit = "Oficina de Seguridad";
                outcomeCode = "INJECT_WHITE_BEAR_SYMBOL";
            }
            case "RUPTURA_CUARTA_PARED" -> {
                handlerUnit = "Departamento Netflix";
                outcomeCode = "BREAK_FOURTH_WALL";
            }
            default -> {
                handlerUnit = "Archivo de Errores";
                outcomeCode = "DISCARD_INPUT";
            }
        }

        return new ClassificationResult(branchType, handlerUnit, outcomeCode);
    }
}
