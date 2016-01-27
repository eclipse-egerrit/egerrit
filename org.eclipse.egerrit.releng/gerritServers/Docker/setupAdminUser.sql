INSERT INTO accounts
  (account_id,preferred_email,registered_on)
VALUES
  (1,'admin@localhost',now());
INSERT INTO account_group_members
  (account_id, group_id)
VALUES (
  (SELECT account_id FROM accounts WHERE preferred_email='admin@localhost'),
  (SELECT group_id FROM account_groups WHERE name='Administrators')
);
INSERT INTO account_external_ids
  (account_id, external_id)
VALUES (
  (SELECT account_id FROM accounts WHERE preferred_email='admin@localhost'),
  'gerrit:admin@localhost'
);
INSERT INTO account_external_ids
  (account_id, external_id, email_address, password)
VALUES (
  (SELECT account_id FROM accounts WHERE preferred_email='admin@localhost'),
  'username:admin',
  'admin@localhost',
  'egerritTest'
);
INSERT INTO account_ssh_keys
  (ssh_public_key,valid,account_id)
VALUES (
 '<%= @userkey %>',
 'Y',
 (SELECT account_id FROM accounts WHERE preferred_email='admin@localhost'),
);
