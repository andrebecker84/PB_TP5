-- Inicia banco com portfólio exemplo equilibrado.
-- ON CONFLICT (ticker) DO NOTHING: seguro para reinicializações com ddl-auto=update —
-- não falha se os registros já existirem (PostgreSQL e H2 2.x compatível).
INSERT INTO ativo_financeiro (ticker, nome, tipo, categoria, valor_investido, quantidade, data_ultima_operacao) VALUES
('PETR4',    'Petrobras',          'ACAO',        'RENDA_VARIAVEL', 15000.00,  400.0, '2024-02-15'),
('VALE3',    'Vale S.A.',          'ACAO',        'RENDA_VARIAVEL', 12000.00,  150.0, '2024-02-18'),
('MXRF11',   'Maxi Renda FII',     'FII',         'RENDA_VARIAVEL',  8500.00,  800.0, '2024-02-10'),
('HGLG11',   'Gerdau Logística',   'FII',         'RENDA_VARIAVEL', 11000.00,   65.0, '2024-02-12'),
('BTC',      'Bitcoin',            'CRIPTOMOEDA', 'CRIPTODIVISA',   25000.00,    0.08,'2024-02-19'),
('ETH',      'Ethereum',           'CRIPTOMOEDA', 'CRIPTODIVISA',   10000.00,    0.8, '2024-02-19'),
('SELIC2029','Tesouro Selic 2029', 'RENDA_FIXA',  'RENDA_FIXA',     30000.00,    2.5, '2024-01-20'),
('IPCA2035', 'Tesouro IPCA+ 2035', 'RENDA_FIXA',  'RENDA_FIXA',     15000.00,    4.0, '2024-01-15'),
('OURO1',    'Ouro Spot',          'OURO',        'ATIVO_REAL',      5000.00,   15.0, '2024-02-05')
ON CONFLICT (ticker) DO NOTHING;
