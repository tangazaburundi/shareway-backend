-- V14: Approuver tous les utilisateurs existants + s'assurer que l'admin a SUPER_ADMIN

-- 1. Approuver tous les users existants (V12 a mis admin_approved=0 par défaut)
UPDATE users SET admin_approved = 1, admin_approved_at = NOW()
WHERE admin_approved = 0;

-- 2. S'assurer que l'admin a bien un rôle SUPER_ADMIN
INSERT INTO admin_roles (id, user_id, role, permissions, granted_by, granted_at)
SELECT UUID(), u.id, 'SUPER_ADMIN', '{"ALL": true}', u.id, NOW()
FROM users u
WHERE u.email = 'sharewaybdi@gmail.com'
  AND NOT EXISTS (
    SELECT 1 FROM admin_roles ar WHERE ar.user_id = u.id
  );
