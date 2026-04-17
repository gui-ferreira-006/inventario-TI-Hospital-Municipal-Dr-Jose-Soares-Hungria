package inventario;

import inventario.model.Setor;
import inventario.repository.SetorRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class InventarioApplication {
	public static void main(String [] args) {

		SpringApplication.run(InventarioApplication.class, args);
	}
	// Esse método faz com que o app rode sozinho assim que ele inicia
	// @Bean
	// public CommandLineRunner testeRapido(SetorRepository repository) {
	// 	return (args) -> {
	// 		// Criando um setor de teste
	// 		Setor setorTI = new Setor();
	// 		setorTI.setNome("Tecnologia da Informação");
	// 		setorTI.setBloco("Administrativo - 2º andar");

	// 		// Salvando o setor no banco de dados
	// 		repository.save(setorTI);

	// 		System.out.println("SETOR DE TI CRIADO COM SUCESSO!");
	// 	};
	// }
}