package com.example.proyecto_semestre_medico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProyectoSemestreMedicoApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProyectoSemestreMedicoApplication.class, args);
	}

}
