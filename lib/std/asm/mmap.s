#  void *mmap(void addr[.length], size_t length, int prot, int flags,
#	int fd, off_t offset);
.equ ADDR, 0
.equ PROT, 0x07
.equ FLAGS, 0x22
.equ FD, -1
.equ OFFSET, 0x0
.equ SYS_MMAP, 222

.section .text
.global _mmap

_mmap:
	sd fp, -16(sp)	# Save old FP
	mv fp, sp	# New FP points to the start of this frame
	sd a0, -24(fp)	# Save used regs
	sd a1, -32(fp)
	sd a2, -40(fp)
	sd a3, -48(fp)
	sd a4, -56(fp)
	sd a5, -64(fp)
	sd a7, -72(fp)

	addi sp, fp, -72 # New stack pointer
	
	mv a0, zero	# addr
	ld a1, 8(fp)	# length
	li a2, PROT	# prot
	li a3, FLAGS	# flags
	li a4, FD	# fd
	li a5, OFFSET	# offset
	li a7, SYS_MMAP	# MMAP sys call number
	ecall		# calls mmap()
	
	sd a0, 0(fp)	# Return value
	
	mv sp, fp	# Restore old SP
	ld fp, -16(sp)	# Load old FP

	ld a0, -24(sp)	# Save used regs
	ld a1, -32(sp)
	ld a2, -40(sp)
	ld a3, -48(sp)
	ld a4, -56(sp)
	ld a5, -64(sp)
	ld a7, -72(sp)
	ret
