package com.github.vitor.rinha2024q1;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TransactionsControllers {
	
	private final TransactionRepository transactionRepository;
	
	private final ClientRepository clientRepository;
	
	private final JdbcTemplate jdbcTemplate;

//	@PostMapping("/clientes/{id}/transacoes")
//	@Transactional()
//	public ResponseEntity<Object> createTransaction(@PathVariable Integer id, @RequestBody TransactionEntity body) {
//		Optional<ClientEntity> findById = clientRepository.findById(id);
//		if(findById.isEmpty()) {
//			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//		}
//		
//		final ClientEntity client = findById.get();
//		long balance = client.getBalance();
//		long limit = client.getLimite();
//		
//		if(TransactionType.c.equals(body.getType())) {
//			balance += body.getAmount();
//		} else {			
//			balance -= body.getAmount();
//			if((balance + limit) < 0) {
//				return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
//			}
//		}
//		
//		body.setClient(client);
//		client.setBalance(balance);
//		
//		CompletableFuture<ClientEntity> saveClientAsync = CompletableFuture.supplyAsync(() -> clientRepository.save(client));
//		CompletableFuture<TransactionEntity> saveTransactionAsync = CompletableFuture.supplyAsync(() -> transactionRepository.save(body));
//		
//		saveClientAsync.join();
//		saveTransactionAsync.join();
//		
//		return ResponseEntity.ok(Map.of("limite", limit, "saldo", balance));
//	}
	
	@PostMapping("/clientes/{id}/transacoes")
    @Transactional
    public ResponseEntity<Object> createTransactionV2(@PathVariable Integer id, @RequestBody TransactionEntity body) {
		if(body.getAmount() < 1 || StringUtils.isEmpty(body.getDescription()) || body.getDescription().length() > 10) {
			return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).build();
		}
        String sql = "SELECT limit_val, current_balance from create_transaction(?, ?, ?, ?)";
        Object[] params = { id, body.getAmount(), body.getType().ordinal(), body.getDescription() };
        int[] argsType = { java.sql.Types.INTEGER, java.sql.Types.BIGINT, java.sql.Types.SMALLINT, java.sql.Types.VARCHAR };
        Map<String, Object> executed = jdbcTemplate.queryForMap(sql, params, argsType);
        
        return ResponseEntity.ok(Map.of("limite", executed.get("limit_val"), "saldo", executed.get("current_balance")));
    }
	
	@Data
	static class Balance {
		private long total;
		
		@JsonProperty("data_extrato")
		private ZonedDateTime statementTime = ZonedDateTime.now();
		
		@JsonProperty("limite")
		private long limit;
	}
	
	@Data
	static class TransactionDTO {
		@JsonProperty("saldo")
		private Balance balance;
		
		@JsonProperty("ultimas_transacoes")
		private List<TransactionEntity> lastTransactions;
	}
	
	@GetMapping("/clientes/{id}/extrato")
	public ResponseEntity<Object> getTransaction(@PathVariable Integer id) {
		CompletableFuture<Optional<ClientEntity>> saveClientAsync = CompletableFuture.supplyAsync(() -> clientRepository.findById(id));
		CompletableFuture<List<TransactionEntity>> saveTransactionAsync = CompletableFuture.supplyAsync(() -> transactionRepository.findFirst10ByClientIdOrderByCreatedAtDesc(id));
		
		Optional<ClientEntity> join = saveClientAsync.join();
		
		if(join.isEmpty()) {
			saveTransactionAsync.cancel(true);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND); 
		}
		
		List<TransactionEntity> find = saveTransactionAsync.join();
		
		ClientEntity client = join.get();
		
		TransactionDTO response = new TransactionDTO();
		response.setLastTransactions(find);
		Balance balance = new Balance();
		balance.setTotal(client.getBalance());
		balance.setLimit(client.getLimite());
		
		response.setBalance(balance);
		
		return ResponseEntity.ok(response);
	}
}
