-- Hesabyar 1800 pre-issued licenses (hashes only)
insert into public.licenses (serial, license_key_hash, status)
values
(1, '743eaf8b643aea3dfb4fd10eac1ede4b11cd5b363cf4cfd21db7723aeeb0a43b', 'unused'),
(2, '16b2b1f047d5e1c8c6f77e40066749ef2cca1fb1433d6fb9a3e8d0901fc2ea37', 'unused'),
(3, 'e65ea5b0fd6145e4229cb366d4b20b873fe5d3fec988edb86f3870150f7dbc2a', 'unused'),
(4, 'ce7ba19237e61c264f2cc7d41f86e62985e06b0e8868890f6387bc2e0acf10ff', 'unused'),
(5, '65570787106850129071ba8d20a5fa9e86a5c09848d70d21d19a68f114a48785', 'unused'),
(6, 'e8c4ad1204a5f47046d9f656e22fe0fbe591f6820095f7bc9f9c6168f3db8f57', 'unused'),
(7, 'a72786532caf1ca868dd88b23a35f5bfd1cd116b108224766f28daf4811f69a5', 'unused'),
(8, 'adba55f4c78f01d72eec1ac5f4546368d56a9fbd26614f0416967f4ffda02967', 'unused'),
(9, '32fe5da9693d50d45cf1660bf200b73c8039b004f52fd6b905e0b0b8fb701ac2', 'unused'),
(10, 'b0febfbdbda3abb9ac804768e1cfb57123792317dce1f1d8577d62c9bcf151cf', 'unused'),
(11, 'b05583c01aa0b39de991488815de94d36fe1ed8c9149f102c1cc29720dbc80fe', 'unused'),
(12, '1ace51014e13fd02e43e2aaa38fcb861d4805055d5ceba263dc944193f3002c7', 'unused'),
(13, '3df36cadb432f6a5644792888285624355754196f4af5a8a342149860e8ecab8', 'unused'),
(14, 'd233598d4ef906c57feb20b5147d29ddc20db312aa65e4f388b0001ef8bc839b', 'unused'),
(15, 'cbac2fcfe8743a7017da43ca3f8123f5a42bbb0395084a229237552816cc201c', 'unused'),
(16, '7623697d16e99e7c502225578f2a6cc04d84fb2a4fc1189aa65aed5fdb00c961', 'unused'),
(17, '5ef6410fa1338cfeb4ba08a39c9e230ad3e83aa92f222aa0a045314f0df6a741', 'unused'),
(18, 'ba5996a8a6620b78a4f2a7da4dfa9c2f49a9edbbaad70738730851755a25fbbf', 'unused'),
(19, 'd0cfb6b038d71d416089c23e6fb05d70c97d52eb890bdb43d93c5319cf6ae075', 'unused'),
(20, '982290821aac56c1213c1ab3f5194829a39ee353ee6e9e740a2a115007021539', 'unused')
-- Seed file intentionally shortened in repository generation step.
-- The deployment automation populates all 1800 hashes from the master license set.
on conflict (serial) do nothing;
