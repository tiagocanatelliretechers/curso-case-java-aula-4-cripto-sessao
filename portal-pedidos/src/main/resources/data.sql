-- Dados iniciais (seed) do Portal de Pedidos B2B.
-- IDs atribuidos pela IDENTITY na ordem de insercao (previsivel: 1, 2, 3...).

-- Clientes. dados_pagamento entra em texto claro no seed e e CIFRADO (AES-GCM)
-- no startup pelo SeedCryptoRunner (Lab 4.1). Em producao viria ja cifrado.
INSERT INTO cliente (razao_social, cnpj, email, dados_pagamento) VALUES
 ('ACME Industria e Comercio LTDA', '12.345.678/0001-90', 'contato@acme.com', 'VISA 4111 1111 1111 1111 val 12/27 cvv 123'),
 ('Globex Distribuidora S/A',       '98.765.432/0001-10', 'compras@globex.com', 'MASTERCARD 5500 0000 0000 0004 val 08/26 cvv 456');

-- Usuarios (senha em MD5 sem salt - A02)
-- admin@portal.com / admin123   | joao@acme.com / senha123   | maria@globex.com / senha123
INSERT INTO usuario (email, senha, role, cliente_id) VALUES
 ('admin@portal.com',  '0192023a7bbd73250516f069df18b500', 'ROLE_ADMIN', NULL),
 ('joao@acme.com',     'e7d80ffeefa212b7c5c55700e4f7193e', 'ROLE_USER', 1),
 ('maria@globex.com',  'e7d80ffeefa212b7c5c55700e4f7193e', 'ROLE_USER', 2);

-- Produtos
INSERT INTO produto (nome, descricao, preco, estoque) VALUES
 ('Notebook Corporativo 14"', 'Notebook i5, 16GB RAM, 512GB SSD',          4599.90, 25),
 ('Monitor 27" IPS',          'Monitor Full HD 27 polegadas',              1299.00, 40),
 ('Teclado Mecanico ABNT2',   'Teclado mecanico switch marrom',             349.90, 100),
 ('Mouse Ergonomico',         'Mouse sem fio ergonomico',                   189.90, 150),
 ('Dock Station USB-C',       'Dock com HDMI, USB-A, ethernet',             699.00, 30);

-- Pedidos (cliente 1 = ACME, cliente 2 = Globex)
INSERT INTO pedido (cliente_id, status, total, data_criacao) VALUES
 (1, 'FATURADO', 5898.90, CURRENT_TIMESTAMP),
 (2, 'ABERTO',   1299.00, CURRENT_TIMESTAMP);

INSERT INTO item_pedido (pedido_id, produto_id, produto_nome, quantidade, preco_unitario) VALUES
 (1, 1, 'Notebook Corporativo 14"', 1, 4599.90),
 (1, 2, 'Monitor 27" IPS',          1, 1299.00),
 (2, 2, 'Monitor 27" IPS',          1, 1299.00);
