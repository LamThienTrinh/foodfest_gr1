-- Active: 1764685608297@@127.0.0.1@5432@foodfest
select * from tags
where tags.tag_name = 'Hải sản';

#lấy tất cả các mapping của dish 'Gà rang gừng'
select dish_tags.*, tags.tag_name from dish_tags
join tags 
on dish_tags.tag_id = tags.tag_id
where dish_id = (
    select dish_id from dishes
    where dish_name = 'Gà rang gừng'
);

select * from dish_tags;

truncate table dish_tags;