/*
 * Cyclos 3.5 clean up script
 * --------------------------
 * 
 * WARNING!!!: Running this script over an existing Cyclos database will 
 * mercilessly delete all users, transactions and all related data,
 * leaving only the configuration. Be careful when you use it.
 * The only user that will be left is the default 'admin'.
 *
 * WARNING 2: After running this script, also remove the WEB-INF/indexes
 * and WEB-INF/cache directories (if any) in order to prevent old data to be
 * retrieved from searches.
 */

/* Delete data*/
delete from brokering_commission_status;
delete from brokerings;
delete from contacts;
delete from reference_history;
delete from refs;
delete from ad_interests;
delete from notification_preferences;
delete from images where subclass in ('ad', 'mbr');
delete from custom_field_values where subclass <> ('admin');
delete from custom_field_values where admin_id <> 1;
delete from alerts;
delete from error_log_entry_parameters;
delete from error_log_entries;
delete from admin_alert_notification_preferences;
delete from admin_preferences_new_members;
delete from admin_preferences_message_categories;
delete from admin_preferences_transfer_types;
delete from admin_preferences_new_pending_payments;
delete from admin_preferences_guarantee_types;
delete from admin_notification_preferences;
update transfers set by_id = null, parent_id = null, transaction_fee_id = null, loan_payment_id = null, account_fee_log_id = null, fee_id = null, receiver_id = null, external_transfer_id = null, chargeback_of_id = null;
update account_fees set enabled_since = current_date where enabled_since is not null;
delete from invoice_payments;
delete from invoices;
delete from account_status;
delete from account_fee_charges;
delete from account_fee_logs;
delete from external_transfers;
delete from loan_payments;
delete from members_loans;
delete from payment_obligation_logs;
delete from payment_obligations;
delete from guarantee_logs;
delete from guarantees;
delete from certification_logs;
delete from certifications;
delete from loans;
delete from tickets;
delete from transfer_authorizations;
delete from transfers;
delete from scheduled_payments;
delete from accounts where subclass = 'M';
delete from operator_groups_max_amount;
delete from members_loan_groups;
delete from members_loans;
delete from loan_groups;
delete from ads;
delete from login_history;
delete from remarks;
delete from group_history_logs;
delete from password_history;
delete from users where username not in ('2348168551964');
delete from messages_to_groups;
delete from messages;
update members set member_broker_id = null, member_id = null;
update groups set member_id = null;
delete from members_channels;
delete from member_records;
delete from pending_members;
delete from transaction_fees where from_member_id is not null or to_member_id is not null;
delete from custom_field_values where field_id in (select id from custom_fields where member_id is not null);
delete from custom_field_possible_values where field_id in (select id from custom_fields where member_id is not null);
delete from custom_fields where member_id is not null;
delete from documents where member_id is not null;
delete from registration_agreement_logs;
/*
delete from service_client_permissions;
delete from service_clients_receive_payment_types;
delete from service_clients_do_payment_types;
delete from service_clients;
*/
delete from custom_field_values where field_id in (select id from custom_fields where transfer_type_id in (select id from transfer_types where fixed_destination_member_id is not null));
delete from custom_field_possible_values where field_id in (select id from custom_fields where transfer_type_id in (select id from transfer_types where fixed_destination_member_id is not null));
delete from custom_fields where transfer_type_id in (select id from transfer_types where fixed_destination_member_id is not null);
delete from transfer_types_channels where transfer_type_id in (select id from transfer_types where fixed_destination_member_id is not null);
delete from groups_chargeback_transfer_types where transfer_type_id in (select id from transfer_types where fixed_destination_member_id is not null);
delete from groups_transfer_types where transfer_type_id in (select id from transfer_types where fixed_destination_member_id is not null);
delete from groups_transfer_types_as_member where transfer_type_id in (select id from transfer_types where fixed_destination_member_id is not null);
delete from transfer_types where fixed_destination_member_id is not null;
delete from default_broker_commissions where broker_id not in (select id from users);
delete from sms_mailings;
delete from member_sms_status;
delete from sms_logs;
delete from members where id not in (select id from users);
delete from permissions where group_id in (select id from groups where subclass = 'O');
delete from groups_transfer_types where group_id in (select id from groups where subclass = 'O');
delete from files where group_id in (select id from groups where subclass = 'O');
delete from group_operator_account_information_permissions;
delete from groups where subclass = 'O'; 

/* Reset the auto increment value
alter table account_status auto_increment=1;
alter table account_fee_charges auto_increment=1;
alter table brokerings auto_increment=1;
alter table contacts auto_increment=1;
alter table reference_history auto_increment=1;
alter table refs auto_increment=1;
alter table ad_interests auto_increment=1;
alter table notification_preferences auto_increment=1;
alter table images auto_increment=1;
alter table custom_field_values auto_increment=1;
alter table custom_field_values auto_increment=1;
alter table alerts auto_increment=1;
alter table error_log_entry_parameters auto_increment=1;
alter table error_log_entries auto_increment=1;
alter table admin_alert_notification_preferences auto_increment=1;
alter table admin_preferences_message_categories auto_increment=1;
alter table admin_preferences_transfer_types auto_increment=1;
alter table admin_notification_preferences auto_increment=1;
alter table account_fee_logs auto_increment=1;
alter table invoices auto_increment=1;
alter table external_transfers auto_increment=1;
alter table payment_obligation_logs auto_increment=1;
alter table payment_obligations auto_increment=1;
alter table guarantee_logs auto_increment=1;
alter table guarantees auto_increment=1;
alter table certification_logs auto_increment=1;
alter table certifications auto_increment=1;
alter table loan_payments auto_increment=1;
alter table loans auto_increment=1;
alter table tickets auto_increment=1;
alter table transfers auto_increment=1;
alter table accounts auto_increment=1;
alter table groups auto_increment=1;
alter table operator_groups_max_amount auto_increment=1;
alter table group_history_logs auto_increment=1;
alter table members_loan_groups auto_increment=1;
alter table members_loans auto_increment=1;
alter table loan_groups auto_increment=1;
alter table ads auto_increment=1;
alter table login_history auto_increment=1;
alter table remarks auto_increment=1;
alter table messages auto_increment=1;
alter table members auto_increment=2;
*/