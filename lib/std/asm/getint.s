.section .text
.global _getint
_getint:
	addi sp, sp, -24
	sd fp, 0(sp)
	mv fp, sp
	addi fp, fp, 24
	addi sp, sp, -8
	sd ra, 0(sp)
	addi sp, sp, -8
	sd a2, 0(sp)
	addi sp, sp, -8
	sd a3, 0(sp)
	addi sp, sp, -8
	sd a4, 0(sp)
	addi sp, sp, -8
	sd a5, 0(sp)
	addi sp, sp, -8
	j L23
L23:
L8:
	mv a3, fp
	li a2, -16
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	mv a3, a2
	call _getchar
	ld a2, 0(sp)
	mv a2, a2
	sd a2, 0(a3)
	mv a2, fp
	li a3, -8
	mv a3, a3
	add a2, a2, a3
	mv a2, a2
	mv a3, a2
	li a2, 0
	sd a2, 0(a3)
L10:
	mv a3, fp
	li a2, -16
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	ld a2, 0(a2)
	mv a3, a2
	li a2, 48
	mv a2, a2
	slt a2, a3, a2
	seqz a2, a2
	mv a2, a2
	mv a4, a2
	mv a3, fp
	li a2, -16
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	ld a2, 0(a2)
	mv a2, a2
	li a3, 57
	mv a3, a3
	slt a2, a3, a2
	seqz a2, a2
	mv a2, a2
	mv a2, a2
	and a2, a4, a2
	mv a2, a2
	bnez a2, L11
L25:
	j L12
L11:
	mv a2, fp
	li a3, -8
	mv a3, a3
	add a2, a2, a3
	mv a2, a2
	mv a4, a2
	mv a2, fp
	li a3, -8
	mv a3, a3
	add a2, a2, a3
	mv a2, a2
	ld a2, 0(a2)
	mv a2, a2
	li a3, 10
	mv a3, a3
	mul a2, a2, a3
	mv a2, a2
	sd a2, 0(a4)
	mv a3, fp
	li a2, -8
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	mv a2, a2
	mv a4, fp
	li a3, -8
	mv a3, a3
	add a3, a4, a3
	mv a3, a3
	ld a3, 0(a3)
	mv a5, a3
	mv a4, fp
	li a3, -16
	mv a3, a3
	add a3, a4, a3
	mv a3, a3
	ld a3, 0(a3)
	mv a3, a3
	li a4, 48
	mv a4, a4
	sub a3, a3, a4
	mv a3, a3
	mv a3, a3
	add a3, a5, a3
	mv a3, a3
	sd a3, 0(a2)
	mv a3, fp
	li a2, -16
	mv a2, a2
	add a2, a3, a2
	mv a2, a2
	mv a3, a2
	call _getchar
	ld a2, 0(sp)
	mv a2, a2
	sd a2, 0(a3)
	j L10
L12:
	mv a3, fp
	mv a2, fp
	li a4, -8
	mv a4, a4
	add a2, a2, a4
	mv a2, a2
	ld a2, 0(a2)
	sd a2, 0(a3)
	j L9
L9:
	j L24
L24:
	addi sp, sp, 8
	ld a5, 0(sp)
	addi sp, sp, 8
	ld a4, 0(sp)
	addi sp, sp, 8
	ld a3, 0(sp)
	addi sp, sp, 8
	ld a2, 0(sp)
	addi sp, sp, 8
	ld ra, 0(sp)
	addi sp, sp, 8
	ld fp, 0(sp)
	addi sp, sp, 24
	ret
