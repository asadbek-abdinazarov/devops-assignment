package uz.javachi.devops_assignment.service;

public interface EmailService {
    void sendEmail(String to, String subject, String body);
}
