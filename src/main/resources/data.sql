-- Sample flights
INSERT INTO flights (flight_number, airline, source, destination, departure_date, departure_time, arrival_date, arrival_time, total_seats, available_seats, price, version)
VALUES ('AI101', 'Air India', 'DEL', 'BOM', '2026-06-10', '08:00:00', '2026-06-10', '10:00:00', 200, 200, 4500.00, 0);

INSERT INTO flights (flight_number, airline, source, destination, departure_date, departure_time, arrival_date, arrival_time, total_seats, available_seats, price, version)
VALUES ('AI202', 'Air India', 'DEL', 'BOM', '2026-06-10', '14:00:00', '2026-06-10', '16:00:00', 200, 200, 5200.00, 0);

INSERT INTO flights (flight_number, airline, source, destination, departure_date, departure_time, arrival_date, arrival_time, total_seats, available_seats, price, version)
VALUES ('6E301', 'IndiGo', 'BOM', 'MAA', '2026-06-10', '11:00:00', '2026-06-10', '13:00:00', 180, 180, 3200.00, 0);

INSERT INTO flights (flight_number, airline, source, destination, departure_date, departure_time, arrival_date, arrival_time, total_seats, available_seats, price, version)
VALUES ('6E302', 'IndiGo', 'BOM', 'MAA', '2026-06-10', '17:00:00', '2026-06-10', '19:00:00', 180, 180, 3800.00, 0);

INSERT INTO flights (flight_number, airline, source, destination, departure_date, departure_time, arrival_date, arrival_time, total_seats, available_seats, price, version)
VALUES ('UK401', 'Vistara', 'DEL', 'MAA', '2026-06-10', '06:00:00', '2026-06-10', '09:00:00', 150, 150, 6500.00, 0);

INSERT INTO flights (flight_number, airline, source, destination, departure_date, departure_time, arrival_date, arrival_time, total_seats, available_seats, price, version)
VALUES ('SG501', 'SpiceJet', 'DEL', 'CCU', '2026-06-10', '07:00:00', '2026-06-10', '09:30:00', 180, 180, 2800.00, 0);

INSERT INTO flights (flight_number, airline, source, destination, departure_date, departure_time, arrival_date, arrival_time, total_seats, available_seats, price, version)
VALUES ('AI601', 'Air India', 'CCU', 'MAA', '2026-06-10', '11:00:00', '2026-06-10', '13:30:00', 200, 200, 3500.00, 0);

INSERT INTO flights (flight_number, airline, source, destination, departure_date, departure_time, arrival_date, arrival_time, total_seats, available_seats, price, version)
VALUES ('6E701', 'IndiGo', 'DEL', 'HYD', '2026-06-10', '09:00:00', '2026-06-10', '11:30:00', 180, 180, 3000.00, 0);

INSERT INTO flights (flight_number, airline, source, destination, departure_date, departure_time, arrival_date, arrival_time, total_seats, available_seats, price, version)
VALUES ('UK801', 'Vistara', 'HYD', 'MAA', '2026-06-10', '13:00:00', '2026-06-10', '14:30:00', 150, 150, 2200.00, 0);

INSERT INTO flights (flight_number, airline, source, destination, departure_date, departure_time, arrival_date, arrival_time, total_seats, available_seats, price, version)
VALUES ('6E000', 'IndiGo', 'BOM', 'CCU', '2026-06-10', '13:00:00', '2026-06-10', '14:30:00', 180, 180, 2000.00, 0);

INSERT INTO flights (flight_number, airline, source, destination, departure_date, departure_time, arrival_date, arrival_time, total_seats, available_seats, price, version)
VALUES ('6E888', 'IndiGo', 'CCU', 'MAA', '2026-06-10', '16:00:00', '2026-06-10', '18:00:00', 180, 180, 3000.00, 0);

-- Seats for AI101 (36 seats)
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'A1', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'A2', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'A3', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'A4', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'A5', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'A6', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'B1', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'B2', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'B3', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'B4', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'B5', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'B6', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'C1', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'C2', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'C3', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'C4', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'C5', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'C6', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'D1', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'D2', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'D3', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'D4', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'D5', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'D6', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'E1', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'E2', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'E3', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'E4', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'E5', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'E6', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'F1', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'F2', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'F3', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'F4', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'F5', TRUE FROM flights f WHERE f.flight_number = 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, 'F6', TRUE FROM flights f WHERE f.flight_number = 'AI101';

-- 12 seats each for other flights
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '1', TRUE FROM flights f WHERE f.flight_number != 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '2', TRUE FROM flights f WHERE f.flight_number != 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '3', TRUE FROM flights f WHERE f.flight_number != 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '4', TRUE FROM flights f WHERE f.flight_number != 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '5', TRUE FROM flights f WHERE f.flight_number != 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '6', TRUE FROM flights f WHERE f.flight_number != 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '7', TRUE FROM flights f WHERE f.flight_number != 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '8', TRUE FROM flights f WHERE f.flight_number != 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '9', TRUE FROM flights f WHERE f.flight_number != 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '10', TRUE FROM flights f WHERE f.flight_number != 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '11', TRUE FROM flights f WHERE f.flight_number != 'AI101';
INSERT INTO seats (flight_id, seat_number, available) SELECT f.id, '12', TRUE FROM flights f WHERE f.flight_number != 'AI101';
