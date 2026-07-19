CREATE TABLE partenaires (
    id VARCHAR(36) PRIMARY KEY,
    nom VARCHAR(200) NOT NULL,
    image_url VARCHAR(500),
    lien_url VARCHAR(500),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_partenaires_actif ON partenaires(actif);
CREATE INDEX idx_partenaires_sort_order ON partenaires(sort_order);
