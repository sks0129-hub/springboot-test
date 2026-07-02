package com.example.certificateportal.employee;

import java.time.LocalDate;

public record Employee(
        String userId,
        String passwordHash,
        String name,
        LocalDate hireDate,
        boolean specialAuth
) {
}
