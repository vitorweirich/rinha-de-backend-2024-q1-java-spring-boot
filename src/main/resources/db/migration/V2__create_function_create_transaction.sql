CREATE OR REPLACE FUNCTION create_transaction(client_id_arg INTEGER, amount_arg BIGINT, type_arg SMALLINT, description_arg VARCHAR)
RETURNS TABLE (limit_val BIGINT, current_balance BIGINT)
AS $$
DECLARE
    current_balance BIGINT;
    limit_val BIGINT;
BEGIN
    BEGIN
        -- Obter o saldo e o limite do cliente
        SELECT balance, limite INTO current_balance, limit_val FROM client_entity WHERE id = client_id_arg FOR UPDATE;

        -- Verificar se o cliente existe
        IF NOT FOUND THEN
            RAISE EXCEPTION SQLSTATE '90404' USING MESSAGE = 'Cliente não encontrado';
        END IF;

        -- -- Verificar se há saldo suficiente para transações do tipo débito
        -- IF type_arg = 0 THEN
        --     IF (current_balance + limit_val) < amount_arg THEN
        --         RAISE EXCEPTION SQLSTATE '90422' USING MESSAGE = 'Saldo insuficiente para esta transação';
        --     END IF;
        -- END IF;

        -- Verificar se há saldo suficiente para transações do tipo débito
        IF type_arg = 0 THEN
            current_balance := current_balance - amount_arg;
            IF current_balance < limit_val THEN
                RAISE EXCEPTION SQLSTATE '90422' USING MESSAGE = 'Saldo insuficiente para esta transação';
            END IF;
        ELSE
            current_balance := current_balance + amount_arg;
        END IF;

        -- -- Atualizar o saldo do cliente
        -- IF type_arg = 0 THEN
        --     current_balance := current_balance - amount_arg;
        -- ELSE
        --     current_balance := current_balance + amount_arg;
        -- END IF;

        UPDATE client_entity SET balance = current_balance WHERE id = client_id_arg;

        -- Inserir a transação
        INSERT INTO transaction_entity (client_id, type, amount, description, created_at) 
        VALUES (client_id_arg, type_arg, amount_arg, description_arg, NOW());

    END;
    
    RETURN QUERY SELECT limit_val, current_balance;
END;
$$
LANGUAGE plpgsql;
