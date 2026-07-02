package com.example.certificateportal.employee;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
public class EmployeeService {

    private static final String EMPLOYEE_CSV = "static/employee.csv";
    private final Map<String, Employee> employees;

    public EmployeeService() {
        this.employees = loadEmployees();
    }

    public Optional<Employee> authenticate(String userId, String rawPassword) {
        Employee employee = employees.get(userId);
        if (employee == null || rawPassword == null) {
            return Optional.empty();
        }

        byte[] enteredPasswordHash = sha256(rawPassword);
        byte[] savedPasswordHash;
        try {
            savedPasswordHash = HexFormat.of().parseHex(employee.passwordHash());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("employee.csv에 올바르지 않은 비밀번호 해시가 있습니다.", exception);
        }

        return MessageDigest.isEqual(enteredPasswordHash, savedPasswordHash)
                ? Optional.of(employee)
                : Optional.empty();
    }

    private Map<String, Employee> loadEmployees() {
        Map<String, Employee> loadedEmployees = new HashMap<>();
        ClassPathResource resource = new ClassPathResource(EMPLOYEE_CSV);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            if (line == null) {
                throw new IllegalStateException("employee.csv가 비어 있습니다.");
            }

            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] columns = line.split(",", -1);
                if (columns.length != 5) {
                    throw new IllegalStateException("employee.csv " + lineNumber + "행의 형식이 올바르지 않습니다.");
                }

                Employee employee = new Employee(
                        columns[0].trim(),
                        columns[1].trim(),
                        columns[2].trim(),
                        LocalDate.parse(columns[3].trim()),
                        "Y".equalsIgnoreCase(columns[4].trim())
                );
                if (loadedEmployees.putIfAbsent(employee.userId(), employee) != null) {
                    throw new IllegalStateException("employee.csv에 중복된 사용자 ID가 있습니다: " + employee.userId());
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("employee.csv를 읽을 수 없습니다.", exception);
        }

        return Collections.unmodifiableMap(loadedEmployees);
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
