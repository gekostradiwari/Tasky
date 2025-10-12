-- Creazione del database
CREATE DATABASE TasksManager;
USE TasksManager;

-- Tabella Dipartimento
CREATE TABLE Dipartimento (
    id_dipartimento INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    numero_dipendenti INT NOT NULL
);
-- Creazione del database (idempotente)
CREATE DATABASE IF NOT EXISTS TasksManager;
USE TasksManager;

-- Tabella Dipartimento
CREATE TABLE IF NOT EXISTS Dipartimento (
    id_dipartimento INT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    numero_dipendenti INT NOT NULL
);

-- Seed default Dipartimento (solo se vuoto)
INSERT INTO Dipartimento (nome, numero_dipendenti)
SELECT 'Default', 0
WHERE NOT EXISTS (SELECT 1 FROM Dipartimento LIMIT 1);

-- Tabella Manager
CREATE TABLE IF NOT EXISTS Manager (
    email VARCHAR(100) PRIMARY KEY,
    nome VARCHAR(50) NOT NULL,
    cognome VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    data_nascita DATE NOT NULL,
    sesso VARCHAR(16) NOT NULL,
    numero_telefono VARCHAR(32) NOT NULL,
    anni_lavorativi INT NOT NULL,
    token VARCHAR(255),
    Dipartimento_id_dipartimento INT,
    FOREIGN KEY (Dipartimento_id_dipartimento) REFERENCES Dipartimento(id_dipartimento) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Tabella Dipendente
CREATE TABLE IF NOT EXISTS Dipendente (
    email VARCHAR(100) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    nome VARCHAR(50) NOT NULL,
    cognome VARCHAR(50) NOT NULL,
    data_nascita DATE NOT NULL,
    sesso VARCHAR(16) NOT NULL,
    numero_telefono VARCHAR(32) NOT NULL,
    token VARCHAR(255),
    Dipartimento_id_dipartimento INT,
    FOREIGN KEY (Dipartimento_id_dipartimento) REFERENCES Dipartimento(id_dipartimento) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Tabella Progetto
CREATE TABLE IF NOT EXISTS Progetto (
    id_progetto INT AUTO_INCREMENT PRIMARY KEY,
    descrizione TEXT NOT NULL,
    budgetIstanziato DECIMAL(15, 2) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    dataInizio DATE NOT NULL,
    dataFine DATE NOT NULL,
    Dipartimento_id_dipartimento INT,
    FOREIGN KEY (Dipartimento_id_dipartimento) REFERENCES Dipartimento(id_dipartimento) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Tabella TASK
CREATE TABLE IF NOT EXISTS TASK (
    id INT AUTO_INCREMENT PRIMARY KEY,
    stato VARCHAR(50) NOT NULL,
    descrizione TEXT NOT NULL,
    data_inizio DATE NOT NULL,
    data_fine DATE NOT NULL,
    Progetto_id_progetto INT,
    Dipendente_email VARCHAR(100),
    Manager_email VARCHAR(100),
    FOREIGN KEY (Progetto_id_progetto) REFERENCES Progetto(id_progetto) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (Dipendente_email) REFERENCES Dipendente(email) ON DELETE CASCADE ON UPDATE CASCADE,
    FOREIGN KEY (Manager_email) REFERENCES Manager(email) ON DELETE CASCADE ON UPDATE CASCADE
);

-- Tabella per sottoscrizioni push (FCM)
CREATE TABLE IF NOT EXISTS push_subscriptions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    role ENUM('Manager','Dipendente') NOT NULL,
    platform VARCHAR(32) NOT NULL DEFAULT 'android',
    fcm_token VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_role (email, role)
);
