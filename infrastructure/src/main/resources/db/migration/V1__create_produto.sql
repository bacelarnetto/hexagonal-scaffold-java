CREATE TABLE produto (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    custo DECIMAL(12,2) NOT NULL,
    margem_percentual DECIMAL(5,2) NOT NULL,
    valor_venda DECIMAL(12,2)
);
