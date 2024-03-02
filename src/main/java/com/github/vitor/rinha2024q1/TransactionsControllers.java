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

import io.micrometer.common.util.StringUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class TransactionsControllers {
	
	private final TransactionRepository transactionRepository;
	
	private final ClientRepository clientRepository;
	
	private final JdbcTemplate jdbcTemplate;

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
	
	@GetMapping("/clientes/{id}/extrato")
	public ResponseEntity<Object> getTransaction(@PathVariable Integer id) {
		CompletableFuture<Optional<ClientEntity>> saveClientAsync = CompletableFuture.supplyAsync(() -> clientRepository.findById(id));
		CompletableFuture<List<TransactionEntity>> saveTransactionAsync = CompletableFuture.supplyAsync(() -> transactionRepository.findFirst10ByClientIdOrderByCreatedAtDesc(id));
		
		Optional<ClientEntity> join = saveClientAsync.join();
		
		if(join.isEmpty()) {
			saveTransactionAsync.cancel(true);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND); 
		}
		
		ClientEntity client = join.get();
		
		return ResponseEntity.ok(
				Map.of("saldo", Map.of("data_extrato", ZonedDateTime.now(), "limite", client.getLimite(), "total", client.getBalance()),
				"ultimas_transacoes", saveTransactionAsync.join().stream().map(t -> Map.of("valor", t.getAmount(), "tipe", t.getType(), "descricao", t.getDescription(), "realizada_em", t.getCreatedAt())).toList()));
	}
}
